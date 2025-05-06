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
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "RANINFO", measType = "CM", operatorName = "VODAFONE")
public class RanInfoDownloadEngine extends DefaultTimeBasedEngine {

    public RanInfoDownloadEngine(ServerIpList eachIp) {
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
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName() + "+" + new SimpleDateFormat("yyyyMMddHHmmss").format(each.getFileCreatedDate()), each);
        }

        List<Map> rawTableList = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableList(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));

        HashSet<String> rawFileList = new HashSet<>();
        for (Map tableData : rawTableList) {
            rawFileList.add(tableData.get("FUNCTIONSUBSETNAME").toString());
        }

        for (RemoteFileObject each : fileList) {
            String eachFileName = connectionInfo.getIp() + "+" + each.getFileName().replace(" ", "_").replace("GSMBTS_Board", "GSM_Board");

            //FileName Filter
            try {

                if (eachFileName.endsWith(".bak")) {
                    continue;
                }

                boolean fileFilter = false;
                for (String neName : rawFileList) {
                    if (eachFileName.contains(neName)) {
                        fileFilter = true;
                        break;
                    }
                }

                //filter for 4GTrx
                if (eachFileName.contains("cellinfoson")) {
                    String subPath = each.getAbsolutePath().replace("//", "/").replace(connectionInfo.getRemotePath().replace("//", "/"), "").replace("/", "");
                    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    if ((subPath.startsWith(today) && !connectionInfo.getElementManager().endsWith("-UPGRADED"))
                            || (subPath.startsWith("cellinfoson") && connectionInfo.getElementManager().endsWith("-UPGRADED"))) {
                        fileFilter = true;
                    } else {
                        fileFilter = false;
                    }
                }

                if (!fileFilter) {
                    continue;
                }

                //add Date
                if (eachFileName.contains("2G_TRX") || eachFileName.contains("cellinfoson")) {
                    eachFileName = eachFileName.replace(".xls", "_" + each.getFileDateWithPattern("yyyyMMdd") + ".xls");
                }

                //edit Date
                if (eachFileName.contains("ConfigurationReport_result")) {
                    String lastPart = eachFileName.split("\\_")[eachFileName.split("\\_").length - 1];
                    eachFileName = eachFileName.replace("_" + lastPart, lastPart);
                }
            } catch (Exception e) {
                continue;
            }

            String localFileName = DownloadApp.LOCALFILEPATH + eachFileName;

            //ReDownload
            String reDownloadCheckFileName = eachFileName + "+" + each.getFileDateWithPattern("yyyyMMddHHmmss");
            if (reDownloadList.containsKey(reDownloadCheckFileName)) {
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    try {
                        if (localFileName.endsWith(".gz")) {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileName);
                        }

                        if (localFileName.endsWith(".zip")) {
                            downloadStatus = UnzipOperation.unzipZipFile(localFileName, eachFileName.split("\\_")[eachFileName.split("\\_").length - 1].substring(0, 8), "LTE_RANINFO.csv", ".html");
                        }
                    } catch (IOException ex) {
                        downloadStatus = false;
                    }
                }

                if (!downloadStatus) {
                    System.err.println("** Error for reDownload file: " + localFileName);
                }

                dbOperationExistFileObject(reDownloadList.get(reDownloadCheckFileName), downloadStatus, each.getPath());
                reDownloadList.remove(reDownloadCheckFileName);
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    try {
                        if (localFileName.endsWith(".gz")) {
                            downloadStatus = UnzipOperation.unzipGzFile(localFileName);
                        }

                        if (localFileName.endsWith(".zip")) {
                            downloadStatus = UnzipOperation.unzipZipFile(localFileName, eachFileName.split("\\_")[eachFileName.split("\\_").length - 1].substring(0, 8), ".csv", ".html");
                        }
                    } catch (IOException ex) {
                        downloadStatus = false;
                    }
                }

                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMdd").parse(eachFileName.split("\\_")[eachFileName.split("\\_").length - 1].substring(0, 8));
                } catch (ParseException ex) {
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                fileHousekeepObj.setDownloadTryCount(downloadStatus ? 1 : 10);
                fileHousekeepObj.setFileDate(fileDate);
                fileHousekeepObj.setFileName(eachFileName);
                fileHousekeepObj.setFileSize(each.getFileSize());
                fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setFileCreatedDate(each.getDate());
                fileHousekeepObj.setDescription(each.getPath());

                dbOperationNonExistFileObject(fileHousekeepObj, each);

                if (!downloadStatus) {
                    System.err.println("** Error for download file: " + localFileName);
                }
            }
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            System.err.println("** Error for reDownload file: " + fileHousekeep.getFileName());
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }
}
