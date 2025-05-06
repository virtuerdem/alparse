/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author EnesTerzi,TurgutSimsek
 */
@DownloadEngine(measType = "PM", systemType = "ALUSDH", operatorName = "VODAFONE")
public class AluSdhDownloadEngine extends DefaultTimeBasedEngine {

    public AluSdhDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {
        setCommitSize(200);
        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (RemoteFileObject each : fileList) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName().replace(":", "_"), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {

            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName().replace(":", "_"), each.getPath());

                String date = each.getFileName().split("\\_")[3];
                String SDate = date.substring(0, date.length() - 2);
                Date fileDate = null;
                try {
                    fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse(SDate);
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
    }
}
