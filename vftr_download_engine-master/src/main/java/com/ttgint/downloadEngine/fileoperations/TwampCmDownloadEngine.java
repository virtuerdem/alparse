/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.LocalFileLib;
import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import com.ttgint.downloadEngine.engines.CopyEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "TWAMP", measType = "CM", operatorName = "VODAFONE")
public class TwampCmDownloadEngine extends CopyEngine {

    public TwampCmDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void copyFiles(Date lastDateFromDb, LocalFileLib fileLib, List<LocalFileObject> files, ServerIpList connectionInfo) {

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (LocalFileObject each : files) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, fileLib.copyFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (LocalFileObject each : files) {
            if (each.getFileName().startsWith("huawei_intf_s1_x2") && each.getFileName().endsWith(".csv")) {
                if (each.getDate().after(lastDateFromDb)) {

                    Date fileDate = new Date();
                    boolean copyStatus = false;
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        fileDate = sdf.parse(each.getFileName().split("\\-", 2)[1].replace(".csv", ""));

                        copyStatus = fileLib.copyFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                    } catch (ParseException ex) {
                        System.err.println(each.getFileName() + " " + ex.getMessage());
                    }

                    FileHousekeep fileHouseKeep = new FileHousekeep();
                    fileHouseKeep.setDownloaded(copyStatus ? 1 : 0);
                    fileHouseKeep.setDownloadTryCount(1);
                    fileHouseKeep.setFileSize(each.getFileSize());
                    fileHouseKeep.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHouseKeep.setFileDate(fileDate);
                    fileHouseKeep.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHouseKeep.setMeasType(DownloadApp.MEASTYPE);
                    fileHouseKeep.setFileName(each.getFileName());
                    fileHouseKeep.setConnectionId(connectionInfo.getConnectionId());
                    fileHouseKeep.setFileCreatedDate(each.getDate());

                    dbOperationNonExistFileObject(fileHouseKeep, each);

                }
            }
        }
    }
}
