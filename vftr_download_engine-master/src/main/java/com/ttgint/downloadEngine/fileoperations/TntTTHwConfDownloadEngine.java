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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "HW2G-CONF", measType = "CM", operatorName = "TURKTELEKOM"),
    @DownloadEngine(systemType = "HW3G-CONF", measType = "CM", operatorName = "TURKTELEKOM"),
    @DownloadEngine(systemType = "HW4G-CONF", measType = "CM", operatorName = "TURKTELEKOM"),
    @DownloadEngine(systemType = "HW5G-CONF", measType = "CM", operatorName = "TURKTELEKOM")})
public class TntTTHwConfDownloadEngine extends DefaultTimeBasedEngine {

    public TntTTHwConfDownloadEngine(ServerIpList eachIp) {
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

                    try {
                        flag = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                        UnzipOperation.unzipGzFile(DownloadApp.LOCALFILEPATH + each.getFileName());
                        dbOperationExistFileObject(fileHousekeep, flag, each.getPath());
                    } catch (Exception ex) {
                        flag = false;
                    }
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {

            if (each.getFileName().endsWith(".gz") && each.getDate().after(getLastDateFromdb())) {

                boolean downloadFlag = false;
                String[] fileNameArr = each.getFileName().split("\\_");
                String neName = each.getFileName().split("\\_")[1];

                if (fileNameArr.length == 4) {

                    if (DownloadApp.SYSTEMTYPE.equals("HW2G-CONF")) {
                        boolean isBscFlag = neName.startsWith("B") && neName.substring(neName.length() - 3).startsWith("H");
                        boolean isBtsFlag = (neName.length() == 6);

                        downloadFlag = (isBscFlag || isBtsFlag);
                    }

                    if (DownloadApp.SYSTEMTYPE.equals("HW3G-CONF")) {
                        boolean isRncFlag = neName.startsWith("N") && neName.substring(neName.length() - 3).startsWith("H");
                        boolean isNodebFlag = ((neName.startsWith("W") || neName.startsWith("U")) && neName.length() == 7);

                        downloadFlag = (isRncFlag || isNodebFlag);
                    }

                    if (DownloadApp.SYSTEMTYPE.equals("HW4G-CONF")) {
                        boolean isEnodebFlag = neName.startsWith("L") && neName.length() == 7;
                        downloadFlag = isEnodebFlag;
                    }

                    if (DownloadApp.SYSTEMTYPE.equals("HW5G-CONF")) {
                        boolean isEnodebFlag = neName.startsWith("G") && neName.length() == 7;
                        downloadFlag = isEnodebFlag;
                    }
                }

                if (downloadFlag) {
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                    try {
                        UnzipOperation.unzipGzFile(DownloadApp.LOCALFILEPATH + each.getFileName());
                    } catch (Exception ex) {
                        downloadStatus = false;
                    }

                    String fileDateStr = fileNameArr[3];
                    fileDateStr = fileDateStr.substring(0, 8);
                    Date fileDate = null;
                    try {
                        fileDate = new SimpleDateFormat("yyyyMMdd").parse(fileDateStr);
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

}
