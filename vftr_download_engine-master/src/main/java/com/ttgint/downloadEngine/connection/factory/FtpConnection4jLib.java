/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.connection.settings.FileInfoEnum;
import com.ttgint.downloadEngine.connection.settings.FileListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author EnesTerzi
 */
public class FtpConnection4jLib extends Connection {

    FTPClient client = new FTPClient();

    private static final int DIRECTORY = 1;
    private static final int FILE = 0;

    protected FtpConnection4jLib(String _ipAdress, String _userName, String _passWord, Integer _port) {
        super(_ipAdress, _userName, _passWord, _port);
    }

    @Override
    public boolean getConnection() {
        try {
            //    System.out.println(_ipAdress + " " + _port);
            client.connect(_ipAdress, _port);
            //    System.out.println("connected");
            //    client.setType(FTPClient.TYPE_BINARY);
            //     System.out.println(_userName + " " + _passWord);
            client.login(_userName, _passWord);
            //  System.out.println("loggined");
            System.out.println("Connected to " + _ipAdress + " via FTP");
            return client.isConnected();
        } catch (Exception ex) {
            //System.out.println("Connection Failed to " + _ipAdress + ". " + ex.getMessage());
            CommonLibrary.errorLogger("**** Connection Failed to " + _ipAdress + " " + ex.getMessage(), 2);
            return false;

        }

    }

    @Override
    public boolean closeConnection() {
        try {
            client.disconnect();
            return true;
        } catch (Exception ex) {
            return false;
        }

    }

    @Override
    public boolean checkConnectionisAlive() {
        return client.isConnected();
    }

    @Override
    public Object getConnectionObject() {
        return client;
    }

    @Override
    public List<RemoteFileObject> readAllFilesWalkinPath(String remotePath) {

        List<RemoteFileObject> willReturnObject = new ArrayList<>();
        Queue<RemoteFileObject> directorylist = new LinkedBlockingQueue<>();
        RemoteFileObject object = null;
        object = new RemoteFileObject(FileInfoEnum.DIRECTORY);
        object.setDirectPath(remotePath);
        directorylist.add(object);
        try {
            while (!directorylist.isEmpty()) {
                object = directorylist.poll();
                FTPFile[] filelist = client.listFiles(object.getPath());

                for (FTPFile each : filelist) {
                    RemoteFileObject objectTemp;
                    if (each.getType() == DIRECTORY) {
                        objectTemp = new RemoteFileObject(FileInfoEnum.DIRECTORY);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(object.getPath());
                        directorylist.add(objectTemp);
                    } else if (each.getType() == FILE) {
//                        System.out.println(each.getName());
                        objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(object.getPath());
                        objectTemp.setFileSize(each.getSize());
                        objectTemp.setFileType();
                        objectTemp.setDate(each.getTimestamp().getTime());
                        willReturnObject.add(objectTemp);
                    }
                }
                object = null;
            }
        } catch (Exception ex) {
            if (ex.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
            }
            return null;
        }
        return willReturnObject;
    }

    @Override
    public boolean downloadFile(String fullLocalPad, String fullRemotePath) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(fullLocalPad));
            client.retrieveFile(fullRemotePath, fos);
            fos.close();
            return true;
        } catch (Exception ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + fullLocalPad + " " + ex.getMessage(), 0);
            return false;
        }
    }

    @Override
    public List<RemoteFileObject> readAllFilesInCurrentPath(String remotePath) {
//        System.out.println("Entered Method");
        List<RemoteFileObject> willReturnObject = new ArrayList<>();
        try {
            FTPFile[] filelist = client.listFiles(remotePath);
//            System.out.println("Remote File size "  +remotePath + " " + filelist.length);
            for (FTPFile each : filelist) {
                RemoteFileObject objectTemp;
                if (each.getType() == FILE) {
                    objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                    objectTemp.setFileName(each.getName());
                    objectTemp.setAbsolutePath(remotePath);
                    objectTemp.setFileSize(each.getSize());
                    objectTemp.setFileType();
                    objectTemp.setDate(each.getTimestamp().getTime());
                    willReturnObject.add(objectTemp);
                }
            }
        } catch (Exception ex) {
            if (ex.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
            }
            ex.printStackTrace();
            return null;
        }
        return willReturnObject.isEmpty() ? null : willReturnObject;
    }

    @Override
    public boolean uploadFile(InputStream input, String fileName, String remotePath) {
        throw new UnsupportedOperationException("use apache lib");
    }

    @Override
    public boolean uploadFile(File input, String remotePath) {
        throw new UnsupportedOperationException("use apache lib");
    }

    @Override
    public boolean downloadFileWithTtgOutputStreamForXml(String fullRemotePath, ModifiedOutputStream output) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readAllFilesWalkingPathWithListener(FileListener listener, String remotePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readAllFilesInCurrentPathWithListener(FileListener listener, String remotePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RemoteFileObject getRemoteFileInfo(String fullRemotePath) {
        RemoteFileObject objectTemp = null;
        try {
            FTPFile each = client.listFiles(fullRemotePath)[0];

            if (each.getType() == FILE) {
                objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                objectTemp.setFileName(each.getName());
                objectTemp.setFileSize(each.getSize());
                objectTemp.setDate(each.getTimestamp().getTime());
            }
        } catch (Exception ex) {
            CommonLibrary.errorLogger("**** getRemoteFileInfo failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 1);
        }
        return objectTemp;
    }
}
