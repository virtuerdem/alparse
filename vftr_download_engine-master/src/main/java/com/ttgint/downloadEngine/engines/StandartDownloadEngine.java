/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.FileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.File;

/**
 *
 * @author EnesTerzi
 */
abstract class StandartDownloadEngine extends AbsDownloadEngine {

    public StandartDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public abstract void setConnectionInfoLib(ConnectionInfo info);

    
    public abstract void onDownload(Connection con, ServerIpList connectionInfo);

    @Override
    public abstract void afterFinishForCurrentThread(Connection con);

    @Override
    public void requestStop() {
        super.requestStop();
    }

    @Override
    public void examineLocalFile(File file) {
        super.examineLocalFile(file);
    }

    @Override
    public void examineRemoteFile(FileObject fileObj) {
        super.examineRemoteFile(fileObj);
    }

    @Override
    public void run() {
        setConnectionInfoLib(getConnectionInfoObject());
        setRemoteConnectionConnection();
        onDownload(getRemoteConnection(), getServerInfo());
        afterFinishForCurrentThread(getRemoteConnection());
        super.run();
    }

}
