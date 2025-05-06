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
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "HWINV-CONF", measType = "CM", operatorName = "TURKTELEKOM")
public class TntTTHwInventoryConfDownloadEngine extends DefaultTimeBasedEngine {

    public TntTTHwInventoryConfDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(
                DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableListActive(
                        DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        HashSet<String> fileNames = new HashSet<>();
        for (Map fileNameFilter : fileNameFilters) {
            if (!fileNameFilter.get("FUNCTIONSUBSETNAME").toString().isBlank()) {
                fileNames.add((String) fileNameFilter.get("FUNCTIONSUBSETNAME"));
            }
        }

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});
        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
        }
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                if (!fileNames.contains(each.getFileName().substring(0, each.getFileName().length() - 24))) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            //ReDownload
            if (reDownloadList.containsKey(each.getFileName())) {
                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                reDownloadList.remove(each.getFileName());
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").parse(each.getFileName().substring(each.getFileName().length() - 23, each.getFileName().length() - 4));
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

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }

    }
}
