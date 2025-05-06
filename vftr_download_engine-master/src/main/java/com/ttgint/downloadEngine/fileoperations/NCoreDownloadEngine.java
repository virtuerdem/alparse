/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author TTGETERZI, erdigurbuz
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "NCORE", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "NSS-SSGW", measType = "PM", operatorName = "VODAFONE")}
)
public class NCoreDownloadEngine extends DefaultTimeBasedEngine {

    public NCoreDownloadEngine(ServerIpList eachIp) {
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
        setCommitSize(10);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");

        HashSet<String> pathList = new HashSet<>();
        Calendar cal = Calendar.getInstance();
        //Add CurrentHour for Path
        pathList.add(sdf.format(cal.getTime()) + "00");
        //Add PreHours for Path
        cal.add(Calendar.HOUR, -1);
        pathList.add(sdf.format(cal.getTime()) + "00");

        int i = 0;
        while (cal.getTime().after(getLastDateFromdb())) {
            i++;
            cal.add(Calendar.HOUR, -1);
            pathList.add(sdf.format(cal.getTime()) + "00");
            if (i > 48) {
                break;
            }
        }

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
            //Add ReDownloadDate for Path
            pathList.add(each.getFileName().substring(2, 12) + "00");
        }

        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " SubPath size : " + pathList.size());
        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String eachDate : pathList) {
            String remotePath = (connectionInfo.getRemotePath() + "/" + eachDate + "_48/").replace("//", "/");
            fileList.addAll(con.readAllFilesWalkinPath(remotePath));
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                if (!(each.getFileName().startsWith("PM"))) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String localFileName = connectionInfo.getIp() + "-" + each.getFileName();
            String fullLocalFileName = DownloadApp.LOCALFILEPATH + localFileName;

            //ReDownload
            if (reDownloadList.containsKey(localFileName)) {
                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());

                if (downloadStatus) {
                    if (fullLocalFileName.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(localFileName), downloadStatus, each.getPath());
                reDownloadList.remove(localFileName);
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());

                if (downloadStatus) {
                    if (fullLocalFileName.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                Date fileDate = new Date();
                try {
                    String date;
                    if (DownloadApp.SYSTEMTYPE.equals("NSS-SSGW")) {
                        date = each.getFileName().split("\\+")[2].substring(3).replace(".", " ");
                        String[] dateArray = date.split("\\ ");
                        date = dateArray[0] + editHoursMinute(dateArray[1], 30);
                    } else {
                        date = each.getFileName().substring(2).split("\\+")[0];
                        String dateDayMontY = date.substring(0, 8);
                        String hourAndMinute = date.substring(8, 12);
                        hourAndMinute = editHoursMinute(hourAndMinute, 15);
                        date = dateDayMontY + hourAndMinute;
                        //date = each.getFileName().substring(2, 14);
                    }
                    fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse(date);
                } catch (ParseException ex) {
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(fileDate);
                fileHousekeepObj.setFileName(localFileName);
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

    public String editHoursMinute(String minute, int timePeriod) {

        int min = Integer.valueOf(minute.substring(2));
        String hour = minute.substring(0, 2);

        String dateMinute = null;
        switch (min / timePeriod) {
            case 0:
                dateMinute = "00";
                break;
            case 1:
                dateMinute = (timePeriod == 15) ? "15" : "30";
                break;
            case 2:
                dateMinute = "30";
                break;
            case 3:
                dateMinute = "45";
                break;
        }
        return (hour + dateMinute);
    }
}
