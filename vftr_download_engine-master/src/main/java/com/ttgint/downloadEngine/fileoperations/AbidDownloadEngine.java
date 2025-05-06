/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.LocalFileLib;
import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.CopyEngine;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "ABID", measType = "CM", operatorName = "VODAFONE")
public class AbidDownloadEngine extends DefaultTimeBasedEngine {

    public AbidDownloadEngine(ServerIpList eachIp) {
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

        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
        }

        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableListActive(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));

        HashSet<String> fileNames = new HashSet<>();
        for (Map fileNameFilter : fileNameFilters) {
            if (!fileNameFilter.get("FUNCTIONSUBSETNAME").toString().isBlank()) {
                fileNames.add((String) fileNameFilter.get("FUNCTIONSUBSETNAME"));
            }
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                if (!fileNames.contains(each.getFileName().split("\\-", 2)[0].trim())) {
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
                    fileDate = new SimpleDateFormat("yyyy-MM-dd").parse(each.getFileName().split("\\-", 2)[1].replace(".txt", ""));
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
