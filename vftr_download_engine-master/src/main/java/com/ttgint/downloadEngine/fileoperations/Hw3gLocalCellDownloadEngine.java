/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(measType = "PM", operatorName = "VODAFONE", systemType = "HW3G-LOCALCELL")
public class Hw3gLocalCellDownloadEngine extends DefaultTimeBasedEngine {

    public Hw3gLocalCellDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (!con.getConnection()) {
            return;
        }
        setCommitSize(200);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        HashSet<String> pathList = new HashSet<>();
        Calendar cal = Calendar.getInstance();
        //Add CurrentDate for Path
        pathList.add(sdf.format(cal.getTime()));
        //Add PreDate for Path
        cal.add(Calendar.DATE, -1);
        pathList.add(sdf.format(cal.getTime()));

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
            //Add ReDownloadDate for Path
            pathList.add(sdf.format(each.getFileDate()));
        }

        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String eachDate : pathList) {
            String remotePath = (connectionInfo.getRemotePath() + "/neexport_" + eachDate + "/").replace("//", "/");
            fileList.addAll(con.readAllFilesWalkinPath(remotePath));
        }

        Date currentDate = new Date();
        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                if (!((each.getFileName().endsWith(".xml") || each.getFileName().endsWith(".xml.gz"))
                        && each.getFileName().split("\\_")[1].startsWith("R")
                        && each.getFileName().split("\\_")[1].split("\\.")[1].startsWith("N")
                        && !each.getFileName().contains("_-_"))) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String localFileName = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();

            //ReDownload
            if (reDownloadList.containsKey(each.getFileName())) {
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                downloadStatus = unCompressFile(downloadStatus, localFileName);

                dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                reDownloadList.remove(each.getFileName());
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMdd.HHmmZ").parse(each.getFileName().substring(1, 19));
                } catch (ParseException ex) {
                }

                if (fileDate.before(currentDate)) {
                    boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                    downloadStatus = unCompressFile(downloadStatus, localFileName);

                    FileHousekeep fileHousekeepObj = new FileHousekeep();
                    fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                    fileHousekeepObj.setDownloadTryCount(1);
                    fileHousekeepObj.setFileDate(fileDate);
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
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }

    private boolean unCompressFile(boolean downloadStatus, String localFileName) {
        boolean status = downloadStatus;
        if (downloadStatus) {
            if (localFileName.endsWith(".gz")) {
                try {
                    UnzipOperation.unzipGzFile(localFileName);
                } catch (IOException ex) {
                    status = false;
                }
            }
        }
        return status;
    }
}
