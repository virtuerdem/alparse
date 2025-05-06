/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(measType = "PM", operatorName = "MCKS", systemType = "GSMA")
public class McksGsmaDownloadEngine extends DefaultTimeBasedEngine {

    public McksGsmaDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (!con.getConnection()) {
            return;
        }
        List<RemoteFileObject> fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            reDownloadList.put(each.getFileName(), each); //Add file for ReDownloadList
        }

        List<Map> maps = DaoUtils.getQueryAsListMap(DownloadQueries.getGsmaTask());
        ArrayList<String> gsmaList = new ArrayList<>();
        for (Map map : maps) {
            gsmaList.add(map.get("FILE_NAME").toString());
        }

        for (RemoteFileObject each : fileList) {
            try {
                if (!each.getFileName().toLowerCase().endsWith(".tmp")
                        && gsmaList.contains(each.getFileName())) {

                    //ReDownload
                    if (reDownloadList.containsKey(each.getFileName())) {
                        String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                        boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                        dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                        reDownloadList.remove(each.getFileName());
                        continue;
                    }

                    //TimeBased Download
                    if (each.getDate().after(getLastDateFromdb())) {
                        Date fileDate = new Date();
                        try {
                            fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse("20" + each.getFileName().split("\\_20")[1].substring(0, 6) + "0000");
                        } catch (ParseException ex) {
                        }

                        String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                        boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

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

            } catch (Exception e) {
            }
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }
}
