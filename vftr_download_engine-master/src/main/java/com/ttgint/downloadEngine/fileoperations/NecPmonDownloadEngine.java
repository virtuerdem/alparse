/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "NEC-PMON", measType = "PM", operatorName = "VODAFONE")
public class NecPmonDownloadEngine extends DefaultTimeBasedEngine {

    public NecPmonDownloadEngine(ServerIpList eachIp) {
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
                    dbOperationExistFileObject(fileHousekeep, con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                FileHousekeep fileHouseKeep = new FileHousekeep();
                fileHouseKeep.setDownloaded(downloadStatus ? 1 : 0);
                fileHouseKeep.setDownloadTryCount(1);
                fileHouseKeep.setFileSize(each.getFileSize());
                fileHouseKeep.setOperatorName(DownloadApp.OPERATORNAME);
                fileHouseKeep.setFileDate(new Date());
                fileHouseKeep.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHouseKeep.setMeasType(DownloadApp.MEASTYPE);
                fileHouseKeep.setFileName(each.getFileName());
                fileHouseKeep.setConnectionId(connectionInfo.getConnectionId());

                dbOperationNonExistFileObject(fileHouseKeep, each);
            }
        }
    }
}
