/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "HWU2020-ALARM", measType = "PM", operatorName = "VODAFONE")
public class Hw2020AlarmDownloadEngine extends DefaultTimeBasedEngine {

    public Hw2020AlarmDownloadEngine(ServerIpList eachIp) {
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
        setCommitSize(10);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        HashSet<String> pathList = new HashSet<>();
        Calendar cal = Calendar.getInstance();
        pathList.add(sdf.format(cal.getTime()));
        //Add PreHours for Path
        cal.add(Calendar.DATE, -1);
        pathList.add(sdf.format(cal.getTime()));
        cal.add(Calendar.DATE, -1);
        pathList.add(sdf.format(cal.getTime()));
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
            pathList.add(each.getFileName().substring(0, 8));
        }

        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " SubPath size : " + pathList.size());
        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String eachDate : pathList) {
            String remotePath = (connectionInfo.getRemotePath() + "/" + eachDate + "/").replace("//", "/");
            fileList.addAll(con.readAllFilesWalkinPath(remotePath));
        }

        HashSet<String> fileNames = new HashSet<>();
        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableListActive(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        for (Map fileNameFilter : fileNameFilters) {
            fileNames.add((String) fileNameFilter.get("FUNCTIONSUBSETNAME"));
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                String fileName = each.getFileName()
                        .replace(each.getFileName().split("\\-")[0] + "-", "")
                        .replace("-" + each.getFileName().split("\\-")[each.getFileName().split("\\-").length - 1], "");
                if (!fileNames.contains(fileName)) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String localFileName = connectionInfo.getIp() + "-" + connectionInfo.getConnectionId() + "-" + each.getFileName();
            String localFileNameFull = DownloadApp.LOCALFILEPATH + localFileName;

            //ReDownload
            if (reDownloadList.containsKey(localFileName)) {
                boolean downloadStatus = con.downloadFile(localFileNameFull, each.getPath());
                if (downloadStatus) {
                    if (localFileNameFull.endsWith(".zip")) {
                        try {
                            downloadStatus = UnzipOperation.unzipZipFile(localFileNameFull, localFileNameFull.replace(".zip", ".csv"));
                        } catch (Exception e) {
                            downloadStatus = false;
                            System.err.println("* Unzip File Error: " + each.getFileName());
                        }
                    } else if (localFileNameFull.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileNameFull);
                        } catch (IOException ex) {
                            downloadStatus = false;
                            System.err.println("* UnGz File Error: " + each.getFileName());
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(localFileName), downloadStatus, each.getPath());
                reDownloadList.remove(localFileName);
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMdd").parse(each.getFileName().substring(0, 8));
                } catch (ParseException ex) {
                }

                boolean downloadStatus = con.downloadFile(localFileNameFull, each.getPath());
                if (downloadStatus) {
                    if (localFileNameFull.endsWith(".zip")) {
                        try {
                            downloadStatus = UnzipOperation.unzipZipFile(localFileNameFull, localFileNameFull.replace(".zip", ".csv"));
                        } catch (Exception e) {
                            downloadStatus = false;
                            System.err.println("* Unzip File Error: " + each.getFileName());
                        }
                    } else if (localFileNameFull.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileNameFull);
                        } catch (IOException ex) {
                            downloadStatus = false;
                            System.err.println("* UnGz File Error: " + each.getFileName());
                        }
                    }
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(fileDate);
                fileHousekeepObj.setFileName(localFileName);
                fileHousekeepObj.setFileSize(each.getFileSize());
                fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setFileCreatedDate(each.getDate());

                dbOperationNonExistFileObject(fileHousekeepObj, each);
            }
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }
}
