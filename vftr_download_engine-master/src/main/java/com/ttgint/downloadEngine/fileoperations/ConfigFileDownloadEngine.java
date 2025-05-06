/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW3G-SITECONFIG"),
    @DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "TILT")
})

public class ConfigFileDownloadEngine extends DefaultStandartEngine {

    public ConfigFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
        setCommitSize(10);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterday2 = new SimpleDateFormat("yyyyMd").format(cal.getTime());

        Boolean connectionstatus = con.getConnection();
        if (connectionstatus == false) {
            return;
        } else {
        }

        List<RemoteFileObject> fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());

        for (RemoteFileObject each : fileList) {
            Boolean downloadStatus = false;
            String fileDate = null;

            switch (DownloadApp.SYSTEMTYPE) {

                case "HW3G-SITECONFIG":
                    if (each.getFileName().contains("GExport_N") && !each.getFileName().contains("GExport_NR")) {
                        downloadStatus = downloadAndUncopmresssFile(fileList, each, con, connectionInfo, today);
                        fileDate = today;
                    }
                    break;
                case "TILT":
                    if (each.getFileName().contains("Tilt")) {
                        downloadStatus = downloadFile(each, con, connectionInfo, yesterday2);
                        fileDate = yesterday2;
                    }
                    break;

                default:
            }

            if (downloadStatus) {
                Date fileDateObj = null;
                try {
                    if (DownloadApp.SYSTEMTYPE.equals("TILT")) {
                        fileDateObj = new SimpleDateFormat("yyyyMd").parse(fileDate);
                    } else {
                        fileDateObj = new SimpleDateFormat("yyyyMMdd").parse(fileDate);
                    }
                } catch (ParseException ex) {
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(1);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(fileDateObj);
                fileHousekeepObj.setFileName(each.getFileName());
                fileHousekeepObj.setFileSize(each.getFileSize());
                fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setFileCreatedDate(each.getDate());

                dbOperationNonExistFileObject(fileHousekeepObj, each);
            }
            downloadStatus = false;

        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
    }

    private boolean downloadFile(RemoteFileObject each, Connection con, ServerIpList connectionInfo, String checkDate) {
        if (each.getFileName().contains(checkDate)) {
            String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();
            return con.downloadFile(fullLocal, connectionInfo.getRemotePath() + each.getFileName());
        }
        return false;
    }

    private boolean downloadAndUncopmresssFile(List<RemoteFileObject> fileList, RemoteFileObject each, Connection con, ServerIpList connectionInfo, String checkDate) {
        boolean status = false;
        String neName = each.getFileName().split("_0\\.|_10\\.")[0];
        RemoteFileObject reference = each;
        if (!each.getFileName().contains(checkDate)) {
            return false;
        }

        //Son gelen file'in inmesi saglaniyor
        for (RemoteFileObject tmpNeObj : fileList) {
            if (tmpNeObj.getFileName().split("_0\\.|_10\\.")[0].equals(neName)) {
                if (tmpNeObj.getDate().after(each.getDate())) {
                    reference = null;
                }
            }
        }
        if (reference != null) {
            String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + reference.getFileName();
            status = con.downloadFile(fullLocal, connectionInfo.getRemotePath() + reference.getFileName());
            if (status) {
                if (fullLocal.endsWith(".gz")) {
                    try {
                        UnzipOperation.unzipGzFile(fullLocal);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        status = false;
                    }
                }
            }
        }
        return status;
    }
}
