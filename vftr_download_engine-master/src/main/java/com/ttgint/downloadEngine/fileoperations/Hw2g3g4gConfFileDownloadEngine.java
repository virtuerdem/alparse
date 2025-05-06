/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ParserUsedNes;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW4G-CONF"),
    @DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW3G-CONF"),
    @DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW2G-CONF")
})

public class Hw2g3g4gConfFileDownloadEngine extends DefaultStandartEngine {

    ArrayList<String> neNameList = null;

    public Hw2g3g4gConfFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
        setCommitSize(10);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (!con.getConnection()) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        String today = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
        cal.add(Calendar.DATE, -1);
        String yesterday = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());

        getNeededNeNames(connectionInfo);

        if (neNameList != null && neNameList.size() > 0) {
            List<RemoteFileObject> remoteFullFileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());
            for (RemoteFileObject each : remoteFullFileList) {
                switch (DownloadApp.SYSTEMTYPE) {
                    case "HW4G-CONF":
                        if ((each.getFileName().startsWith("GExport_SR")
                                || each.getFileName().startsWith("GExport_GL")
                                || each.getFileName().startsWith("GExport_GU")
                                || each.getFileName().startsWith("GExport_NR"))) {
                            downloadAndUncopmresssFile(remoteFullFileList, each, con, connectionInfo, today, today);
                        }
                        break;
                    case "HW3G-CONF":
                        if (each.getFileName().startsWith("GExport_R")) {
                            downloadAndUncopmresssFile(remoteFullFileList, each, con, connectionInfo, today, today);
                        }
                        break;
                    case "HW2G-CONF":
                        if (each.getFileName().startsWith("GExport_B")) {
                            downloadAndUncopmresssFile(remoteFullFileList, each, con, connectionInfo, today, yesterday);
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        if (con != null) {
            if (con.checkConnectionisAlive()) {
                con.closeConnection();
            }
        }
    }

    private void getNeededNeNames(ServerIpList connectionInfo) {
        neNameList = new ArrayList<>();
        String[] parameters = {"systemType", "serverIp"};
        Object[] values = {DownloadApp.SYSTEMTYPE.replace("-CONF", ""), connectionInfo.getIp()};
        List<ParserUsedNes> getIpList = DaoUtils.getObject(ParserUsedNes.class, "hwConf", parameters, values);
        for (ParserUsedNes usedNes : getIpList) {
            neNameList.add(usedNes.getNeName());
        }
    }

    private void downloadAndUncopmresssFile(List<RemoteFileObject> fileList, RemoteFileObject each, Connection con, ServerIpList connectionInfo, String checkDate, String insertDate) {
        String neName = each.getFileName()
                .replace(each.getFileName().split("\\_")[0] + "_", "")
                .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1], "")
                .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 2], "");

        if (!each.getFileName().contains(checkDate) || !neNameList.contains(neName)) {
            return;
        }

        //Son gelen file'in inmesi saglaniyor
        for (RemoteFileObject tmpNeObj : fileList) {
            String tmpNeName = tmpNeObj.getFileName()
                    .replace(tmpNeObj.getFileName().split("\\_")[0] + "_", "")
                    .replace("_" + tmpNeObj.getFileName().split("\\_")[tmpNeObj.getFileName().split("\\_").length - 1], "")
                    .replace("_" + tmpNeObj.getFileName().split("\\_")[tmpNeObj.getFileName().split("\\_").length - 2], "");

            if (tmpNeName.equals(neName)) {
                if (tmpNeObj.getDate().after(each.getDate())) {
                    return;
                }
            }
        }

        String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();
        boolean status = con.downloadFile(fullLocal, connectionInfo.getRemotePath() + each.getFileName());

        if (status && each.getFileName().endsWith(".gz")) {
            try {
                UnzipOperation.unzipGzFile(fullLocal);
            } catch (IOException ex) {
                System.err.println("Unzip Error: " + connectionInfo.getIp() + " " + each.getFileName());
                new File(fullLocal).delete();
                return;
            }
        }

        Date fileDateObj = new Date();
        try {
            fileDateObj = new SimpleDateFormat("yyyyMMdd").parse(each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1].substring(0, 8));
        } catch (ParseException ex) {
        }

        FileHousekeep fileHousekeepObj = new FileHousekeep();
        fileHousekeepObj.setDownloaded(status ? 1 : 0);
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
}
