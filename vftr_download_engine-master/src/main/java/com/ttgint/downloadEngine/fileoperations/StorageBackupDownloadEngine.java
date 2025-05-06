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
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "STORAGE-BACKUP", measType = "PM", operatorName = "VODAFONE")
public class StorageBackupDownloadEngine extends DefaultTimeBasedEngine {

    public StorageBackupDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {
        for (RemoteFileObject each : fileList) {
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadstatus =
                        con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                if (downloadstatus) {
                    examineRemoteFile(each);
                }
            }

        }
    }

}
