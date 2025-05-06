/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.util.List;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngine(systemType = "CELLSTATE", measType = "CM", operatorName = "VODAFONE")
public class CellStateDownloadEngine extends DefaultTimeBasedEngine {

    public CellStateDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        for (RemoteFileObject each : fileList) {

//          boolean startFlag = (each.getFileName().startsWith("cellstate") || each.getFileName().startsWith("sitename")); // 4g düzeldiğnde
            boolean startFlag = each.getFileName().startsWith("cellstate");
            boolean endWithFlag = each.getFileName().endsWith(".txt");

            if (startFlag && endWithFlag && each.getDate().after(getLastDateFromdb())) {

                String fullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
                con.downloadFile(fullPath, each.getPath());
                examineRemoteFile(each);
            }
        }
    }
}
