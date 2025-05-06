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
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ErdiGurbuz
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "ANKRLINK", measType = "PM", operatorName = "HTK")}
)
public class AnkRLinkGDownloadEngine extends DefaultTimeBasedEngine {

    public AnkRLinkGDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBasedPostgreSql",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (RemoteFileObject each : fileList) {
            if (each.getDate().after(getLastDateFromdb())) {
                Date fileDate = each.getDate();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(new SimpleDateFormat("yyyyMMddHHmmss").format(each.getDate()));
                } catch (ParseException ex) {
                }

                String fullPath = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "-" + each.getFileName();
                boolean downloadStatus = con.downloadFile(fullPath, each.getPath());

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
        for (FileHousekeep fileHousekeepObj : fromHousekeepReDownloadList) {
            Boolean isExists = false;
            for (RemoteFileObject each : fileList) {
                isExists = fileHousekeepObj.getFileName().equals(each.getFileName());
                if (isExists) {
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "-" + each.getFileName(), each.getPath());
                    dbOperationExistFileObject(fileHousekeepObj, downloadStatus, each.getPath());
                    break;
                }
            }
            if (!isExists) { // file not found in ftp 
                dbOperationExistFileObject(fileHousekeepObj, false, null);
            }
        }

    }

}
