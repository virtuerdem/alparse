/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW3G-NODEBCONFIG")
public class Hw3gNodebConfigFileDownloadEngine extends DefaultStandartEngine {

    public Hw3gNodebConfigFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        con.getConnection();

        List<RemoteFileObject> fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());
        Date currentDate = new Date();

        for (RemoteFileObject each : fileList) {
            String fullRemotePath = connectionInfo.getRemotePath() + "/" + each.getFileName();
            String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();

            Date fileDate = each.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(fileDate);
            cal.add(Calendar.MINUTE, 45);
            fileDate = cal.getTime();

            if (each.getFileName().endsWith(".txt") && each.getFileName().contains("NodeB")) {
                if (fileDate.before(currentDate)) {
                    boolean status = con.downloadFile(fullLocal, fullRemotePath);
                    System.out.println("Download: " + connectionInfo.getIp() + "+" + each.getFileName() + " " + status);
                } else {
                    System.out.println("Not download: " + connectionInfo.getIp() + "+" + each.getFileName() + " -> " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(fileDate));
                }
            }

        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
    }

}
