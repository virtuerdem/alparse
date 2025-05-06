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
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author TurgutSimsek
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "TWAMP", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "TWAMP", measType = "PM", operatorName = "KKTC-TELSIM")
})
public class TwampDownloadEngine extends DefaultTimeBasedEngine {

    public TwampDownloadEngine(ServerIpList eachIp) {
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
                    String parentName = new File(each.getPath()).getParentFile().getName();
                    String localFilePath = DownloadApp.LOCALFILEPATH + connectionInfo.getConnectionId() + "_" + parentName + "_" + each.getFileName();

                    boolean downloadStatus = con.downloadFile(localFilePath, each.getPath());
                    if (downloadStatus && localFilePath.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(localFilePath);
                        } catch (IOException ex) {
                            downloadStatus = false;
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

        for (RemoteFileObject each : fileList) {

            if (each.getDate().after(getLastDateFromdb())) {

                if (!each.getFileName().startsWith("test-20")) {
                    continue;
                }

                String parentName = new File(each.getPath()).getParentFile().getName();
                String localFilePath = DownloadApp.LOCALFILEPATH + connectionInfo.getConnectionId() + "_" + parentName + "_" + each.getFileName();

                boolean downloadStatus = con.downloadFile(localFilePath, each.getPath());

                if (downloadStatus && localFilePath.endsWith(".gz")) {
                    try {
                        UnzipOperation.unzipGzFile(localFilePath);
                    } catch (IOException ex) {
                        downloadStatus = false;
                    }
                }

                Date fileDate = null;
                try {
                    String fileDateStr = each.getFileName().split("\\-")[1].replace(".csv", "");
                    fileDate = sdf.parse(fileDateStr);

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
