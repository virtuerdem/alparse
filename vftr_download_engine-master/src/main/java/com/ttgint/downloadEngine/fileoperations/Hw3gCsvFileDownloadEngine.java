/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "HW3Gcsv", operatorName = "KKTC-TELSIM", measType = "PM")
public class Hw3gCsvFileDownloadEngine extends DefaultStandartEngine {

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    public Hw3gCsvFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo, List<FileHousekeep> list) {
        if (con.getConnection() == false) {
            System.out.println("Connection Failed to :" + connectionInfo.getIp());
            return;

        }
        for (FileHousekeep each : list) {
            String remoteFileName = each.getFileName().split("\\-")[1];
            String remoteExt = "pmexport_" + formatter.format(each.getFileDate()) + "/";
            String fullLocalPath = DownloadApp.LOCALFILEPATH + each.getFileName();
            String fullRemote = connectionInfo.getRemotePath() + remoteExt + remoteFileName;
            boolean downloadStatus = con.downloadFile(fullLocalPath, fullRemote);

            if (downloadStatus) {
                if (fullLocalPath.endsWith(".gz")) {
                    try {
                        UnzipOperation.unzipGzFile(fullLocalPath);
                    } catch (IOException ex) {
                        downloadStatus = false;
                    }
                }
            }
            dbOperationExistFileObject(each, downloadStatus, fullRemote);
        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

}
