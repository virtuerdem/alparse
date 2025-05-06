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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngine(systemType = "ULAK4G", measType = "CM", operatorName = "VODAFONE")
public class Ulak4gCMDownloadEngine extends DefaultTimeBasedEngine {

    public Ulak4gCMDownloadEngine(ServerIpList eachIp) {
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

        System.out.println("*Connection Id: " + connectionInfo.getConnectionId()
                + ", Connection Ip: " + connectionInfo.getIp()
                + ", File Size: " + fileList.size()
                + ", ReDownload Size: " + fromHousekeepReDownloadList.size());

        for (RemoteFileObject each : fileList) {
            if (!each.getDate().after(getLastDateFromdb())) {
                continue;
            }

            try {
                String strDate = each.getFileName().split("_")[1].substring(0, 10);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date fileDate = sdf.parse(strDate);

                String fullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(fullPath, each.getPath());

                if (downloadStatus && fullPath.endsWith(".gz")) {
                    try {
                        UnzipOperation.unzipGzFile(fullPath);
                    } catch (IOException ex) {
                        downloadStatus = false;
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

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        for (FileHousekeep fileHousekeepObj : fromHousekeepReDownloadList) {
            Boolean isExists = false;
            for (RemoteFileObject each : fileList) {
                isExists = fileHousekeepObj.getFileName().equals(each.getFileName());
                if (isExists) {
                    String fullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
                    boolean downloadStatus = con.downloadFile(fullPath, each.getPath());

                    if (downloadStatus && fullPath.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullPath);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                    dbOperationExistFileObject(fileHousekeepObj, downloadStatus, each.getPath());
                    break;
                }
            }
            if (!isExists) {
                dbOperationExistFileObject(fileHousekeepObj, false, null);
            }
        }
    }
}
