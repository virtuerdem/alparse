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
import com.ttgint.downloadEngine.hibernate.pojos.ParserUsedNes;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.main.DownloadApp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(measType = "CM", operatorName = "VODAFONE", systemType = "HW3G-CONFIG")
public class Hw3gConfigFileDownloadEngine extends DefaultStandartEngine {

    public Hw3gConfigFileDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    private String neName;

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP: ConnectionLibs.ApacheLibSFTP);
        setCommitSize(10);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        List<ParserUsedNes> list = DaoUtils.getObject(ParserUsedNes.class, "hw3gconfig", new String[]{"neIp"}, new Object[]{connectionInfo.getIp()});
        if (list.isEmpty()) {
            return;
        }
        neName = list.get(0).getNeName();

        FtpConnectionApacheLib connection = (FtpConnectionApacheLib) con;
        // connection.changeFtpConfig(FTPClientConfig.SYST_VMS);

        if (connection.getConnection() == false) {
            System.out.println("Connection Failed to " + connectionInfo.getIp());
            return;
        }
        try {
            connection.setFileType(FTP.BINARY_FILE_TYPE);
//            connection.setTransferMode(FTP.COMPRESSED_TRANSFER_MODE);
        } catch (IOException ex) {
            Logger.getLogger(Hw3gConfigFileDownloadEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        String dateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
        boolean downloadStatus = false;

        FTPClient clientObject = (FTPClient) connection.getConnectionObject();

        String versionaPath = String.format(connectionInfo.getRemotePath(), "version_a");
        String versionbPath = String.format(connectionInfo.getRemotePath(), "version_b");
        String downloadedFilename = null;

        String[] filelist = null;

        try {
            filelist = clientObject.listNames(versionbPath);
            if (filelist != null) {
                for (String each : filelist) //  remoteFileList.addAll(con.readAllFilesInCurrentPath(versionbPath));
                {
                    if (each.contains(dateString)) {
                        downloadedFilename = each;
                        String fullLocal = DownloadApp.LOCALFILEPATH + each;
                        try {
                            InputStream input = clientObject.retrieveFileStream(versionbPath + each);
                            FileOutputStream output = new FileOutputStream(new File(fullLocal));
                            int len = 0;

                            byte[] blocks = new byte[1024];

                            while ((len = input.read(blocks)) != -1) {
                                output.write(blocks, 0, len);
                            }
                            downloadStatus = true;
                        } catch (Exception e) {
                            downloadStatus = false;
                            e.printStackTrace();
                        }

                        break;
                    }
                }
            }

        } catch (IOException ex) {

        }

        if (downloadStatus == false) {
            //   System.out.println("Download failed trying another path");
            filelist = null;

            try {
                filelist = clientObject.listNames(versionaPath);
                if (filelist != null) {
                    for (String each : filelist) //  remoteFileList.addAll(con.readAllFilesInCurrentPath(versionbPath));
                    {
                        if (each.contains(dateString)) {
                            downloadedFilename = each;
                            String fullLocal = DownloadApp.LOCALFILEPATH + each;
                            try {
                                InputStream input = clientObject.retrieveFileStream(versionaPath + each);
                                FileOutputStream output = new FileOutputStream(new File(fullLocal));
                                int len = 0;

                                byte[] blocks = new byte[1024];

                                while ((len = input.read(blocks)) != -1) {
                                    output.write(blocks, 0, len);
                                }
                                downloadStatus = true;
                            } catch (Exception e) {
                                downloadStatus = false;
                                e.printStackTrace();
                            }

                            break;
                        }
                    }
                }
            } catch (IOException ex) {
            }

        }

        if (downloadStatus) {
            String localFileName = DownloadApp.LOCALFILEPATH + downloadedFilename;
            try {
                File archiveFIle = new File(localFileName);
                ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(archiveFIle));
                ZipArchiveEntry entry;
                while ((entry = archive.getNextZipEntry()) != null) {

                    if (entry.isDirectory() == false) {
                        String newFileName = entry.getName();

                        newFileName = newFileName.replace(".txt", "_" + neName + ".txt");
                        String fullLocalPath = DownloadApp.LOCALFILEPATH + newFileName;

                        File file = new File(fullLocalPath);
                        int len = 0;

                        byte[] blocks = new byte[1024];
                        try {
                            FileOutputStream out = new FileOutputStream(file);

                            while ((len = archive.read(blocks)) != -1) {
                                out.write(blocks, 0, len);
                            }

                            out.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        archiveFIle.delete();
                    }
                }
                archive.close();
                archiveFIle.delete();

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        con.closeConnection();
    }

}
