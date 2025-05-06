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
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "CONFLOGS", measType = "CM", operatorName = "VODAFONE")
public class ConfLogsFileDownloadEngine extends DefaultStandartEngine {

    public ConfLogsFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Boolean connectionstatus = con.getConnection();
        if (connectionstatus == false) {
            return;
        }

        List<Map> list = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableList(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));

        List<RemoteFileObject> fileList = new ArrayList<>();
        //path bulunamadi
        try {
            fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath() + today + "/" + "userlogs");
            fileList.addAll(con.readAllFilesInCurrentPath(connectionInfo.getRemotePath() + today + "/" + "nelogs"));
        } catch (Exception ex) {
            System.out.println(" * " + connectionInfo.getIp() + " not getting files: " + connectionInfo.getRemotePath() + today + "/" + "*");
        }

        if (fileList == null) {
            return;
        }

        for (RemoteFileObject each : fileList) {
            boolean flag = false;
            for (Map tableData : list) {
                if (each.getFileName().contains((String) tableData.get("FUNCTIONSUBSETNAME"))) {
                    flag = true;
                    break;
                }
            }

            if ((each.getFileName().endsWith(".txt") || each.getFileName().endsWith(".csv")) && flag) {

                String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();
                boolean status = con.downloadFile(fullLocal, each.getAbsolutePath() + "/" + each.getFileName());
                if (status) {
                    try {
                        Date fileDate = each.getDate();
                        if (fileDate == null) {
                            fileDate = new Date();
                        }

                        FileHousekeep fileHousekeepObj = new FileHousekeep();
                        fileHousekeepObj.setDownloaded(1);
                        fileHousekeepObj.setDownloadTryCount(1);
                        fileHousekeepObj.setFileDate(new SimpleDateFormat("yyyyMMdd").parse(new SimpleDateFormat("yyyyMMdd").format(fileDate)));
                        fileHousekeepObj.setFileName(each.getFileName());
                        fileHousekeepObj.setFileSize(each.getFileSize());
                        fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                        fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                        fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                        fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                        fileHousekeepObj.setFileCreatedDate(each.getDate());

                        dbOperationNonExistFileObject(fileHousekeepObj, each);
                    } catch (Exception ex) {

                    }
                }
            }
        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {

    }

}
