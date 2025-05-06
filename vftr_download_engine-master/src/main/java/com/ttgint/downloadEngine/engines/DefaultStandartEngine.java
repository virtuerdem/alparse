/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.util.Calendar;
import java.util.List;
import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGETERZI
 */
public abstract class DefaultStandartEngine extends StandartDownloadEngine {

    public DefaultStandartEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public abstract void setConnectionInfoLib(ConnectionInfo info);

    public void onDownload(Connection con, ServerIpList connectionInfo, List<FileHousekeep> list) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -30);
        String query = "standartDownloadQuery";

        //4G'de sonradan sahalar birden gelince çok geriden data işlemek zorunda kalıyor
        //O yuzden son 5 saati çeken query set edildi
        if (DownloadApp.OPERATORNAME.equals("VODAFONE")
                && (DownloadApp.SYSTEMTYPE.equals("HW4G") || DownloadApp.SYSTEMTYPE.equals("HW5G"))) {
            cal.add(Calendar.MINUTE, -30);
            query = "standartDownloadQueryHW4G";
        }

        String[] parameters = {"fileDate", "systemType", "operatorName", "connection"};
        Object[] values = {cal.getTime(), DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME, connectionInfo.getConnectionId()};
        List<FileHousekeep> filelist = DaoUtils.getObject(FileHousekeep.class, query, parameters, values);

        onDownload(con, connectionInfo, filelist);
    }

    @Override
    public abstract void afterFinishForCurrentThread(Connection con);

}
