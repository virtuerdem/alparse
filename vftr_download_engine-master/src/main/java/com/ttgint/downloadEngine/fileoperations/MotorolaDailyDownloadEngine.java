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
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "MOTOROLA-DAILY", measType = "PM", operatorName = "VODAFONE")
public class MotorolaDailyDownloadEngine extends DefaultStandartEngine {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd");

    public MotorolaDailyDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (con.getConnection() == false) {
            System.out.println("Connection failed to : " + connectionInfo.getIp());
        }
        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.DATE, -1);
        String remotePath = "/usr/omc/ne_data/unload_stats/unload_dir." + dateFormatter.format(currentTime.getTime()) + "/";
        List<Map> list = DaoUtils.getQueryAsListMap(DownloadQueries.getOmcAndTableNameQuery(connectionInfo.getIp()));
        List<RemoteFileObject> remoteFileList = con.readAllFilesWalkinPath(remotePath);
        System.out.println(connectionInfo.getIp() + " remote file size : " + remoteFileList.size());
        for (Map each : list) {
            String neName = (String) each.get("NE_NAME");
            String tableName = (String) each.get("TABLE_NAME");
            tableName = tableName.toLowerCase();
            for (RemoteFileObject eachRemote : remoteFileList) {

//                System.out.println(eachRemote.getFileName());
                if (eachRemote.getFileName().contains(tableName)
                        && !eachRemote.getFileName().contains("lmtl_statistics")
                        && !eachRemote.getFileName().contains("entity")) {

                    String fullLocal = DownloadApp.LOCALFILEPATH + neName + "+" + eachRemote.getFileName();
                    boolean status = con.downloadFile(fullLocal, eachRemote.getPath());
                    if (status) {
                        try {
                            UnzipOperation.unzipZFile(fullLocal);
                        } catch (IOException ex) {

                        }
                    }
                    break;
                }
            }

        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

}
