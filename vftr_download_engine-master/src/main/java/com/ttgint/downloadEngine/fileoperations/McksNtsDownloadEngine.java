/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
@DownloadEngines(downloadEngines = {
    @DownloadEngine(measType = "PM", operatorName = "KKTCELL", systemType = "CDR"),
    @DownloadEngine(measType = "PM", operatorName = "TELSIM", systemType = "CDR"),
    @DownloadEngine(measType = "PM", operatorName = "KKTCELL", systemType = "CDR_NTS"),
    @DownloadEngine(measType = "PM", operatorName = "TELSIM", systemType = "CDR_NTS")
})
public class McksNtsDownloadEngine extends DefaultTimeBasedEngine {

    public McksNtsDownloadEngine(ServerIpList eachIp) {
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

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            reDownloadList.put(each.getFileName(), each); //Add file for ReDownloadList
        }

        List<Map> functionSubsetName = DaoUtils.getQueryAsListMap(
                DownloadQueries.getFunctionSubsetNames((DownloadApp.SYSTEMTYPE.contains("NTS") ? "NTS_PARSER" : "MCKS_PARSER"),
                        DownloadApp.OPERATORNAME, DownloadApp.SYSTEMTYPE, DownloadApp.MEASTYPE));
        ArrayList<String> functionSubsetNames = new ArrayList<>();
        for (Map fsm : functionSubsetName) {
            functionSubsetNames.add(fsm.get("FUNCTIONSUBSETNAME").toString());
        }

        long timeBased = 0L;
        long reDownload = 0L;
        for (RemoteFileObject each : fileList) {
            try {
                boolean brokenFile = false;
                String[] splittedName = each.getFileName().split("\\_");
                if (each.getFileName().toLowerCase().endsWith(".tmp")) {
                    brokenFile = true;
                } else if (!splittedName[0].trim().equals(DownloadApp.OPERATORNAME)) {
                    brokenFile = true;
                } else if (!functionSubsetNames.contains(each.getFileName().split("\\_20")[0].split("\\_", 2)[1].trim())) {
                    brokenFile = true;
                } else if (splittedName[splittedName.length - 3].trim().length() != 14
                        || splittedName[splittedName.length - 2].trim().length() != 14) {
                    brokenFile = true;
                } else if (splittedName[splittedName.length - 1].replace(".csv", "").length() != 4) {
                    brokenFile = true;
                } else {
                    try {
                        new SimpleDateFormat("yyyyMMddHHmmss").parse(splittedName[splittedName.length - 3].trim());
                        new SimpleDateFormat("yyyyMMddHHmmss").parse(splittedName[splittedName.length - 2].trim());
                    } catch (ParseException ex) {
                        brokenFile = true;
                    }
                }

                if (!brokenFile) {
                    //ReDownload
                    if (reDownloadList.containsKey(each.getFileName())) {
                        boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                        if (downloadStatus) {
                            reDownload++;
                        }

                        dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                        reDownloadList.remove(each.getFileName());
                        continue;
                    }

                    //TimeBased Download
                    if (each.getDate().after(getLastDateFromdb())) {
                        Date fileDate = new Date();
                        try {
                            fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse("20" + each.getFileName().split("\\_20")[2].substring(0, 10));
                        } catch (ParseException ex) {
                        }

                        boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                        if (downloadStatus) {
                            timeBased++;
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

            } catch (Exception e) {
                System.err.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " "
                        + each.getFileName() + " " + e.getMessage());
            }
        }
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath()
                + " FileHousekeep size: " + fromHousekeepReDownloadList.size()
                + " ReDownloaded size: " + reDownload
                + " ReadedFile size: " + fileList.size()
                + " TimeBaseded size: " + timeBased);
        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }
}
