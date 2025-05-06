/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
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
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "HWCS", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW2G", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW3G", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW5G", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HWCS", measType = "PM", operatorName = "KKTC-TELSIM"),
    @DownloadEngine(systemType = "HW2G", measType = "PM", operatorName = "KKTC-TELSIM"),
    @DownloadEngine(systemType = "HW3G", measType = "PM", operatorName = "KKTC-TELSIM"),
    @DownloadEngine(systemType = "HW4G", measType = "PM", operatorName = "KKTC-TELSIM")})
public class Hw3g2gPmDownloadEngine extends DefaultStandartEngine {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

    public Hw3g2gPmDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo, List<FileHousekeep> list) {
        if (!con.getConnection()) {
            return;
        }

        if (connectionInfo.getPort() == 21) {
            FTPClient clientObject = (FTPClient) con.getConnectionObject();
            try {
                if (DownloadApp.OPERATORNAME.equals("KKTC-TELSIM")) {
                    clientObject.setFileTransferMode(FTPClient.COMPRESSED_TRANSFER_MODE);
                }
                clientObject.setFileType(FTPClient.BINARY_FILE_TYPE);
            } catch (IOException ex) {
            }
        }

        for (FileHousekeep each : list) {
            String neName = each.getFileName().replace(each.getFileName().split("\\_")[0] + "_", "")
                    .replace(".xml.gz", "")
                    .replace(".xml", "");

            if (neName.contains("P00")) {
                neName = neName.replace("_" + neName.split("\\_")[neName.split("\\_").length - 1], "");
            }

            if (DownloadApp.SYSTEMTYPE.equals("HWCS") && neName.contains(".")) {
                neName = neName.split("\\.")[0];
            }

            String fullRemotePath = (connectionInfo.getRemotePath()
                    + "/neexport_" + dateFormatter.format(each.getFileDate())
                    + "/" + neName
                    + "/" + each.getFileName()).replace("//", "/");
            String fullFilePath = DownloadApp.LOCALFILEPATH + each.getFileName();
            boolean downloadStatus = con.downloadFile(fullFilePath, fullRemotePath);

            if (downloadStatus && fullFilePath.endsWith(".gz")) {
                try {
                    UnzipOperation.unzipGzFile(fullFilePath);
                } catch (IOException ex) {
                    downloadStatus = false;
                }
            }
            dbOperationExistFileObject(each, downloadStatus, fullRemotePath);
        }

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
