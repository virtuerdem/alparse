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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "HW4G", measType = "PM", operatorName = "VODAFONE")
public class HuaweiPmDownloadEngine extends DefaultTimeBasedEngine {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

    public HuaweiPmDownloadEngine(ServerIpList eachIp) {
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

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -30);
        String query = "standartDownloadQuery";

        if (DownloadApp.SYSTEMTYPE.equals("HW4G") || DownloadApp.SYSTEMTYPE.equals("HW5G")) {
            cal.add(Calendar.MINUTE, -30);
            query = "standartDownloadQueryHW4G";
        }

        String[] parameters = {"fileDate", "systemType", "operatorName", "connection"};
        Object[] values = {cal.getTime(), DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME, connectionInfo.getConnectionId()};
        List<FileHousekeep> filelist = DaoUtils.getObject(FileHousekeep.class, query, parameters, values);

        for (FileHousekeep each : filelist) {
            String neName = each.getFileName().replace(each.getFileName().split("\\_")[0] + "_", "")
                    .replace(".xml.gz", "")
                    .replace(".xml", "");

            if (neName.contains("P00")) {
                neName = neName.replace("_" + neName.split("\\_")[neName.split("\\_").length - 1], "");
            }

            String fullRemotePath = (connectionInfo.getRemotePath()
                    + "/neexport_" + dateFormatter.format(each.getFileDate())
                    + "/" + neName
                    + "/" + each.getFileName()).replace("//", "/");
            String fullFilePath = DownloadApp.LOCALFILEPATH + each.getFileName();
            boolean downloadStatus = con.downloadFile(fullFilePath, fullRemotePath);

            if (downloadStatus && fullFilePath.endsWith(".gz")) {
                try {
                    UnzipOperation.unzipGzFile(fullFilePath);
                } catch (IOException ex) {
                    downloadStatus = false;
                }
            }
            dbOperationExistFileObject(each, downloadStatus, fullRemotePath);
        }

        //missedFileCheck 
        String[] params = {"fileDate", "systemType", "operatorName", "measType", "connection"};
        Object[] vals = {cal.getTime(), DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, connectionInfo.getConnectionId()};
        List<String> fileDiffList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadFileCheck", params, vals)
                .stream().map(FileHousekeep::getFileName).collect(Collectors.toList());

        HashSet<String> pathList = new HashSet<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String passHour0 = new SimpleDateFormat("HHmm+").format(calendar.getTime());
        calendar.add(Calendar.HOUR, -1);
        String passHour1 = new SimpleDateFormat("HHmm+").format(calendar.getTime());
        pathList.add("/neexport_" + dateFormatter.format(calendar.getTime()));
        calendar.add(Calendar.HOUR, -1);
        String passHour2 = new SimpleDateFormat("HHmm+").format(calendar.getTime());
        pathList.add("/neexport_" + dateFormatter.format(calendar.getTime()));
        calendar.add(Calendar.HOUR, -1);
        String passHour3 = new SimpleDateFormat("HHmm+").format(calendar.getTime());

        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String eachDate : pathList) {
            String remotePath = (connectionInfo.getRemotePath() + "/" + eachDate + "/").replace("//", "/");
            fileList.addAll(con.readAllFilesWalkinPath(remotePath).stream()
                    .filter(e -> !fileDiffList.contains(e.getFileName())
                    && !e.getFileName().contains(passHour0) && !e.getFileName().contains(passHour1) && !e.getFileName().contains(passHour2) && !e.getFileName().contains(passHour3)
                    && (e.getPath().contains("/GL") || e.getPath().contains("/GU") || e.getPath().contains("/SR") || e.getPath().contains("/NR")))
                    .collect(Collectors.toList()));
        }

        for (RemoteFileObject file : fileList) {
            try {
                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(1);
                fileHousekeepObj.setDownloadTryCount(11);
                fileHousekeepObj.setFileDate(new SimpleDateFormat("yyyyMMdd.HHmm").parse(file.getFileName().substring(1, 14)));
                fileHousekeepObj.setFileName(file.getFileName());
                fileHousekeepObj.setFileSize(file.getFileSize());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType("MISS-" + DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                fileHousekeepObj.setFileCreatedDate(file.getDate());

                dbOperationNonExistFileObject(fileHousekeepObj, file);

            } catch (Exception e) {
            }
        }
    }
}
