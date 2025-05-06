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
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "TEST", measType = "TEST", operatorName = "TEST")
public class TestDownloadEngine extends DefaultStandartEngine {

    public TestDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
        setCommitSize(10);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        System.out.println(con.getConnection());
        List<RemoteFileObject> fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());

        for (RemoteFileObject each : fileList) {
            String localFullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
            System.out.println(localFullPath);
//            con.downloadFile(localFullPath, each.getPath());
//            FileHousekeep fileHouseKeep = FileHouseKeepCreater.create().setDownloaded(1)
//                    .setDownloadTryCount(1).setFileSize(each.getFileSize()).setOperatorName(DownloadApp.OPERATORNAME).setFileDate(new Date())
//                    .setSystemType(DownloadApp.SYSTEMTYPE).setFileName(each.getFileName()).setConnectionId(connectionInfo.getConnectionId())
//                    .build();
//
//            dbOperationNonExistFileObject(fileHouseKeep);
//            File asfasf = new File(DownloadApp.LOCALFILEPATH + fileHouseKeep.getFileId() + " ;" + each.getFileName());
//            try {
//                java.nio.file.Files.move(new File(localFullPath).toPath(), asfasf.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            } catch (IOException ex) {
//            }
        }

    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

}
