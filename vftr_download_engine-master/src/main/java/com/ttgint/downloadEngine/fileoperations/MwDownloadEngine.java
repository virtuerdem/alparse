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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 * @author TurgutSimsek
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "MW", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "MW", measType = "PM", operatorName = "KKTC-TELSIM")})
public class MwDownloadEngine extends DefaultTimeBasedEngine {

    public MwDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (RemoteFileObject each : fileList) {
                //NE_Report dosyalari bugun disinda sıkıstirilmis durumda FTP de mevcut
                flag = fileHousekeep.getFileName().equals(each.getFileName()) || each.getFileName().contains(fileHousekeep.getFileName().replace(".csv", ""));
                if (flag) {
                    String remotePath = each.getPath();
                    String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();
                    //NE_Report dosyalari bugun disinda sıkıstirilmis durumda FTP de mevcut
                    Calendar midnight = new GregorianCalendar();
                    midnight.set(Calendar.HOUR_OF_DAY, 0);
                    midnight.set(Calendar.MINUTE, 0);
                    if (fileHousekeep.getFileDate().before(midnight.getTime())) {
                        if (remotePath.startsWith("NE_Report")) {
                            remotePath = remotePath.replace(".csv", ".tar.gz");
                        }
                    }
                    boolean status = con.downloadFile(fullLocal, remotePath);
                    if (status) {
                        if (fullLocal.endsWith(".gz")) {
                            try {
                                UnzipOperation.uncompressTarGz(new File(fullLocal), DownloadApp.LOCALFILEPATH);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                status = false;
                            }
                        }
                    }
                    dbOperationExistFileObject(fileHousekeep, status, remotePath);
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {

            if (each.getDate().after(getLastDateFromdb()) && !each.getFileName().endsWith(".tar.gz")
                    && (each.getFileName().contains("NE_Report")
                    || each.getFileName().contains("Microwave "))) {

                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName(), each.getPath());
                String dateDayMontY;
                String hourAndMinute;
                String[] dateArray;
                // Microwave Link Report_09-07-2016_10-13-37.csv
                if (each.getFileName().contains("Microwave")) {
                    dateDayMontY = each.getFileName().split("\\_")[1];
                    hourAndMinute = each.getFileName().split("\\_")[2].replace(".csv", "");
                    dateArray = hourAndMinute.split("\\-");
                } else {
                    //NE_Report_2017-03-28_11-15-22.csv
                    dateDayMontY = each.getFileName().split("\\_")[2];
                    hourAndMinute = each.getFileName().split("\\_")[3].replace(".csv", "");
                    dateArray = hourAndMinute.split("\\-");
                }

                String minute = editHoursMinute(dateArray[1], 15);
                String date = dateDayMontY + " " + dateArray[0] + minute;
                Date fileDate = null;
                try {
                    if (each.getFileName().contains("Microwave")) {
                        fileDate = sdf.parse(date);
                    } else {
                        fileDate = sdf2.parse(date);
                    }
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

    public String editHoursMinute(String minute, int timePeriod) {

        int min = Integer.valueOf(minute);
        //  String hour = minute.substring(0, 2);
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
        return dateMinute;
    }

}
