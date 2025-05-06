/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "FEMTO", measType = "PM", operatorName = "VODAFONE")
public class FemtoPmDownloadEngine extends DefaultTimeBasedEngine {

    public FemtoPmDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (RemoteFileObject each : fileList) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                    if (downloadStatus) {
                        if (each.getFileName().endsWith(".gz")) {
                            try {
                                UnzipOperation.unzipGzFile(DownloadApp.LOCALFILEPATH + each.getFileName(), DownloadApp.LOCALFILEPATH + each.getFileName().replace(".gz", ""));

                            } catch (Exception ex) {
                                downloadStatus = false;
                            }
                        } else if (each.getFileName().endsWith(".tgz")) {
                            try {
                                UnzipOperation.uncompresssTgz(new File(DownloadApp.LOCALFILEPATH + each.getFileName()), DownloadApp.LOCALFILEPATH);
                            } catch (Exception ex) {
                                downloadStatus = false;
                                new File(DownloadApp.LOCALFILEPATH + each.getFileName()).delete();
                            }
                        }
                    }
                    dbOperationExistFileObject(fileHousekeep, downloadStatus, each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {
            if (!each.getFileName().endsWith(".gz") && !each.getFileName().endsWith(".tgz")) {
                continue;
            }
            if (each.getDate().after(getLastDateFromdb())) {
                String fullLocal = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(fullLocal, each.getPath());

                String uncompressFileName = "";
                if (downloadStatus) {
                    if (each.getFileName().endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullLocal, DownloadApp.LOCALFILEPATH + each.getFileName().replace(".gz", ""));
                            uncompressFileName = each.getFileName().replace(".gz", "");
                        } catch (Exception ex) {
                            downloadStatus = false;
                            ex.printStackTrace();
                        }
                    } else if (each.getFileName().endsWith(".tgz")) {
                        try {
                            uncompressFileName = UnzipOperation.uncompresssTgz(new File(fullLocal), DownloadApp.LOCALFILEPATH);
                        } catch (Exception ex) {
                            downloadStatus = false;
                            new File(fullLocal).delete();
                            System.out.println("Unzip error: " + each.getAbsolutePath() + "/" + each.getFileName());
                        }
                    }
                }

                Date fileDate = null;
                if (downloadStatus) {
                    String date = uncompressFileName.split("\\+")[0].replace(".", "");
                    try {
                        fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse(date.substring(1, date.length()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    try {
                        fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse(new SimpleDateFormat("yyyyMMddHHmm").format(new Date()));
                    } catch (ParseException ex) {
                    }
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
    }
}
