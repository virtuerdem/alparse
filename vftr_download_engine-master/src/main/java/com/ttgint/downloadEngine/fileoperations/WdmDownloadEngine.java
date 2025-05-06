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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngine(systemType = "WDM", measType = "PM", operatorName = "VODAFONE")
public class WdmDownloadEngine extends DefaultTimeBasedEngine {

    public WdmDownloadEngine(ServerIpList eachIp) {
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

        HashSet<String> fileNames = new HashSet<>();
        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(DownloadQueries.getParserRawTableListNeTypes(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        for (Map fileNameFilter : fileNameFilters) {
            fileNames.add((String) fileNameFilter.get("NE_TYPE"));
        }

        for (RemoteFileObject each : fileList) {
            //FileName Filter
            try {
                if (!(each.getFileName().endsWith(".csv") || each.getFileName().endsWith(".txt"))) {
                    continue;
                }

                boolean fileFilter = false;
                for (String fileNameFilter : fileNames) {
                    if (each.getFileName().replace(" ", "_").contains(fileNameFilter)) {
                        fileFilter = true;
                        break;
                    }
                }

                if (fileFilter && each.getFileName().contains("NWCfg")) {
                    if (each.getAbsolutePath().contains("/script/Schedule_")) {
                        fileFilter = true;
                    } else {
                        fileFilter = false;
                    }
                }

                if (!fileFilter) {
                    continue;
                }

                if (each.getFileName().endsWith(".txt") && each.getFileName().contains("NWCfg")) {
                    each.setFileName(getFileDateAsString(each) + "_" + each.getFileName());
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
                Date fileDate = getFileDate(getFileDateAsString(each));

                if (fileDate != null) {
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

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

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }

    private String getFileDateAsString(RemoteFileObject file) {
        String fileName = file.getFileName();
        String fileDateStr = "";

        try {
            if (fileName.endsWith(".csv") && fileName.contains("NE_Report")) {
                //NE_Report_2018-10-24_11-29-05.csv
                fileDateStr = fileName.split("\\_")[2].replace("-", "");
            } else if (fileName.endsWith(".csv") && fileName.startsWith("PM_IG")) {
                //PM_IG41022_15_201809050945Z_01.csv
                fileDateStr = fileName.split("\\_")[3].replace("Z", "");
            } else if (fileName.endsWith(".txt") && (fileName.contains("_pfm_WDM_") || fileName.contains("_pfm_SDH_"))
                    && fileName.split("\\_")[3].length() > 8) {
                //Schedule_pfm_WDM_2018081409022525361.txt
                fileDateStr = fileName.split("\\_")[3].split("\\.")[0];
                fileDateStr = fileDateStr.substring(0, 8);
            } else if (fileName.endsWith(".txt") && fileName.contains("NWCfg")) {
                fileDateStr = new SimpleDateFormat("yyyyMMdd").format(file.getDate());
            }
        } catch (Exception e) {
        }
        return fileDateStr;
    }

    private Date getFileDate(String fileDateStr) {

        Date date = null;
        if (fileDateStr.length() == 8) {
            try {
                date = new SimpleDateFormat("yyyyMMdd").parse(fileDateStr);
            } catch (ParseException ex) {
                Logger.getLogger(WdmDownloadEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (fileDateStr.length() == 12) {

            try {
                date = new SimpleDateFormat("yyyyMMddHHmm").parse(fileDateStr);
            } catch (ParseException ex1) {
                Logger.getLogger(WdmDownloadEngine.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        return date;
    }

}
