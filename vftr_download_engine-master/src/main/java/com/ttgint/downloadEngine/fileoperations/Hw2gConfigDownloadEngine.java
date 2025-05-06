/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.main.DownloadApp;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "HW2G-CONFIG", measType = "CM", operatorName = "VODAFONE")
public class Hw2gConfigDownloadEngine extends DefaultStandartEngine {

    public Hw2gConfigDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
        setCommitSize(10);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        con.getConnection();
        List<RemoteFileObject> fileList = con.readAllFilesInCurrentPath(connectionInfo.getRemotePath());

        for (RemoteFileObject each : fileList) {
            if (each.getFileName().startsWith("B") && each.getFileName().contains("_data.txt")) {
                String fullRemotePath = connectionInfo.getRemotePath() + each.getFileName();
                String fullLocal = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "+" + each.getFileName();
                boolean status = con.downloadFile(fullLocal, fullRemotePath);
            }
            
        }

    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

}