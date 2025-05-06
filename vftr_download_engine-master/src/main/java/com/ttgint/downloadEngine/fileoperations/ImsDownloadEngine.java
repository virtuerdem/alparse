/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author TurgutSimsek
 */
@DownloadEngine(operatorName = "VODAFONE", systemType = "IMS", measType = "PM")
public class ImsDownloadEngine extends DefaultTimeBasedEngine {

    public ImsDownloadEngine(ServerIpList eachIp) {
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

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        List<String> listNeName = DaoUtils.getObject(String.class, "getNeBySystem", new String[]{"systemType", "operatorName"},
                new Object[]{DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME});

        HashSet<String> pathList = new HashSet<>();
        pathList = getPathList(listNeName, connectionInfo);

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
            //Add ReDownloadPath for Path
            String subPath = (connectionInfo.getRemotePath() + "/"
                    + "neexport_" + new SimpleDateFormat("yyyyMMdd").format(each.getFileDate()) + "/"
                    + each.getFileName()
                            .replace(each.getFileName().split("\\_", 2)[0] + "_", "")
                            .replace(".xml.gz", "")
                            .replace(".xml", "") + "/").replace("//", "/");
            pathList.add(subPath);
        }

        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String path : pathList) {
            try {
                fileList.addAll(con.readAllFilesInCurrentPath(path));
            } catch (Exception e) {
            }
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                String neNamePath = "=" + each.getPath().split("\\/")[each.getPath().split("\\/").length - 2] + "=";
                boolean fileFilter = false;
                for (String neName : listNeName) {
                    if (neNamePath.contains("=" + neName + "=")) {
                        fileFilter = true;
                        break;
                    }
                }

                if (!fileFilter) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            //ReDownload
            if (reDownloadList.containsKey(each.getFileName())) {
                String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    if (localFileName.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                reDownloadList.remove(each.getFileName());

            } else //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    if (localFileName.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMdd.HHmmZ").parse(each.getFileName().substring(1, 19));
                } catch (ParseException ex) {
                }

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

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

    private HashSet<String> getPathList(List<String> listNeName, ServerIpList connectionInfo) {
        HashSet<String> pathList = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String path = "";
        for (String neName : listNeName) {
            for (int i = 0; i < 5; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -i);
                path = connectionInfo.getRemotePath() + "/" + "neexport_" + sdf.format(cal.getTime()) + "/" + neName + "/";
                pathList.add(path.replace("//", "/"));
            }
        }
        return pathList;
    }
}
