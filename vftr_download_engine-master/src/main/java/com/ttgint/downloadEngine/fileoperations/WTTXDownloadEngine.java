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
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
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
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "WTTX", measType = "PM", operatorName = "VODAFONE")
public class WTTXDownloadEngine extends DefaultTimeBasedEngine {

    public WTTXDownloadEngine(ServerIpList eachIp) {
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
            if (!each.getFileName().endsWith(".zip")) {
                continue;
            }

            try {
                String strDate = each.getFileName().split("_")[2].substring(0, 8);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                Date fileDate = sdf.parse(strDate);

                String fullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(fullPath, each.getPath());

                if (downloadStatus) {
                    try {
                        UnzipOperation.unzipZipFile(fullPath);
                    } catch (Exception e) {
                        e.printStackTrace();
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
                    dbOperationExistFileObject(fileHousekeepObj, downloadStatus, each.getPath());

                    if (downloadStatus) {
                        try {
                            UnzipOperation.unzipZipFile(fullPath);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                }
            }
            if (!isExists) {
                dbOperationExistFileObject(fileHousekeepObj, false, null);
            }
        }
    }
}
