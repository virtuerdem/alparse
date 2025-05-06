/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.List;

/**
 *
 * @author TTGETERZI
 */
public abstract class DefaultTimeBasedEngine extends TimeBasedDownloadEngine {

    public DefaultTimeBasedEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public abstract void setConnectionInfoLib(ConnectionInfo info);

    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (con.getConnection() == false) {
            return;
        }
        //System.out.println("Reading Files from " + connectionInfo.getIp() + ":" + connectionInfo.getRemotePath());
        List<RemoteFileObject> list = con.readAllFilesWalkinPath(connectionInfo.getRemotePath());
        //System.out.println("List Size from " + connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " : " + list.size());
        onDownlaod(con, connectionInfo, list);
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        if (con != null) {
            if (con.checkConnectionisAlive()) {
                con.closeConnection();
            }
        }
    }

}
