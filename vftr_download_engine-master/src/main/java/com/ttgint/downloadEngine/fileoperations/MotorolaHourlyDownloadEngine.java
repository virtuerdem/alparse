/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.FtpConnectionApacheLib;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.util.Calendar;
import org.apache.commons.net.ftp.FTP;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "MOTOROLA-HOURLY", measType = "PM", operatorName = "VODAFONE")
public class MotorolaHourlyDownloadEngine extends DefaultStandartEngine {

    private final SimpleDateFormat dateFormatterForPath = new SimpleDateFormat("yyyy.MM.dd");

    public MotorolaHourlyDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        FtpConnectionApacheLib cs = (FtpConnectionApacheLib) con;
        cs.getConnection();
        try {
            cs.setFileType(FTP.BINARY_FILE_TYPE);
            cs.setTransferMode(FTP.COMPRESSED_TRANSFER_MODE);
        } catch (IOException ex) {
            return;
            //capture error
        }
        con.setByteBuffer(2048);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        String[] parameters = {"fileDate", "systemType", "operatorName", "connection"};
        Object[] values = {cal.getTime(), DownloadApp.EDITTEDSYSTEMTYPE, DownloadApp.OPERATORNAME, connectionInfo.getConnectionId()};
        List<FileHousekeep> filelist = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQuery", parameters, values);
        //    System.out.println("File size " + filelist.size());

        for (FileHousekeep each : filelist) {
            String neName = each.getFileName().split("\\-")[0];
            String fileName = each.getFileName().split("\\-")[1];
            String dateStringforPath = dateFormatterForPath.format(each.getFileDate());
            String remoteFileName = each.getFileName().split("\\-")[1];

            String fullLocal = DownloadApp.LOCALFILEPATH + neName + "+" + fileName;
            String fullRemote = connectionInfo.getRemotePath() + dateStringforPath + "/" + remoteFileName;
            boolean status = cs.downloadFile(fullLocal, fullRemote);

            if (status) {
                try {
                    UnzipOperation.unzipZFile(fullLocal);
                } catch (IOException ex) {
                    status = false;
                }
            }
            dbOperationExistFileObject(each, status, fullRemote);
        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }
}
