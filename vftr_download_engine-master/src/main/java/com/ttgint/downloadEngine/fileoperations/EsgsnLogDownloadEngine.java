/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.util.Date;

/**
 *
 * @author TurgutSimsek
 */
@DownloadEngine(systemType = "ESGSN-LOG", measType = "PM", operatorName = "VODAFONE")
public class EsgsnLogDownloadEngine extends DefaultTimeBasedEngine {

    public EsgsnLogDownloadEngine(ServerIpList eachIp) {
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
                // each.getFileName() + "+" + connectionInfo.getIp()
                flag = fileHousekeep.getFileName().split("\\+")[0].equals(each.getFileName());
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName().split("/")[each.getFileName().split("/").length - 1] + "+" + connectionInfo.getIp() + "/" + each.getFileName() + "+" + connectionInfo.getIp(), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        boolean fileFormat;
        Date fileDate = null;
        for (RemoteFileObject each : fileList) {

            fileFormat = each.getFileName().startsWith("A") & each.getFileName().contains("_ebs");

            if (fileFormat) {
                String fDate = each.getFileName().split("\\-")[0].split("\\+")[0];
                String Date = fDate.split("\\.")[1].substring(0, 2) + "00";
                try {
                    fileDate = new SimpleDateFormat("yyyyMMddhhmm").parse(fDate.substring(1, fDate.indexOf(".")) + Date);
                } catch (ParseException ex) {
                }

                if (each.getDate().after(getLastDateFromdb())) {

                    new File(DownloadApp.LOCALFILEPATH + each.getFileName().split("/")[each.getFileName().split("/").length - 1] + "+" + connectionInfo.getIp() + "/").mkdirs();
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName().split("/")[each.getFileName().split("/").length - 1] + "+" + connectionInfo.getIp() + "/" + each.getFileName() + "+" + connectionInfo.getIp(), each.getPath());

                    FileHousekeep fileHouseKeep = new FileHousekeep();
                    fileHouseKeep.setDownloaded(downloadStatus ? 1 : 0);
                    fileHouseKeep.setDownloadTryCount(1);
                    fileHouseKeep.setFileSize(each.getFileSize());
                    fileHouseKeep.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHouseKeep.setFileDate(fileDate);
                    fileHouseKeep.setFileCreatedDate(each.getDate());
                    fileHouseKeep.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHouseKeep.setMeasType(DownloadApp.MEASTYPE);
                    fileHouseKeep.setFileName(each.getFileName() + "+" + connectionInfo.getIp());
                    fileHouseKeep.setConnectionId(connectionInfo.getConnectionId());

                    dbOperationNonExistFileObject(fileHouseKeep, each);
                }
            }
        }
    }
}
