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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author EnesTerzi
 */
public class FtpConnectionApacheLib extends Connection {

    FTPClient _ftpObj = new FTPClient();

    protected FtpConnectionApacheLib(String _ipAdress, String _userName, String _passWord, Integer _port) {
        super(_ipAdress, _userName, _passWord, _port);
    }

    public void setFileType(int type) throws IOException {
        _ftpObj.setFileType(type);
    }

    public void setTransferMode(int mode) throws IOException {
        _ftpObj.setFileTransferMode(mode);
    }

    @Override
    public boolean getConnection() {
        boolean status;
        try {
            _ftpObj.connect(_ipAdress, _port);
            //  _ftpObj.setFileType(FTP.COMPRESSED_TRANSFER_MODE);
            _ftpObj.enterLocalPassiveMode();
            _ftpObj.setConnectTimeout(5000);
            _ftpObj.setControlKeepAliveTimeout(720);
            isConnected = true;
            status = true;
        } catch (IOException ex) {
            //System.out.println("Connection Failed to " + _ipAdress + ". " + ex.getMessage());
            CommonLibrary.errorLogger("**** Connection Failed to " + _ipAdress + " " + ex.getMessage() + " " + ex.toString(), 2);
            status = false;
        }
        if (status) {
            try {

                if (_userName.equals("anonymous")) {
                    _passWord = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
                }

                status = _ftpObj.login(_userName, _passWord);
                if (status == false) {
                    //System.out.println("Connection Failed to " + _ipAdress + ". " + "Auth error");
                    CommonLibrary.errorLogger("**** Connection Failed to " + _ipAdress + " Auth error", 2);
                } else {
                    System.out.println("Connected to " + _ipAdress + " via FTP");
                    isLogin = true;
                    setFileType(FTPClient.BINARY_FILE_TYPE);
                }
            } catch (IOException e) {
                //System.out.println("Connection Failed to " + _ipAdress + ". " + e.getMessage());
                CommonLibrary.errorLogger("**** Connection Failed to " + _ipAdress + " " + e.getMessage() + " " + e.toString(), 2);
                status = false;
            }
        }
        return status;
    }

    @Override
    public boolean downloadFile(String fullLocalPad, String fullRemotePath) {
        InputStream input = null;
        try {
            input = _ftpObj.retrieveFileStream(fullRemotePath);
            int returnCode = _ftpObj.getReplyCode();
            if (input == null || returnCode == 550) {
                return false;
            }

            try (FileOutputStream output = new FileOutputStream(fullLocalPad)) {
                byte[] bytesArray = new byte[CURRENT_FILE_BYTE_BUFFER];
                int bytesRead;
                while ((bytesRead = input.read(bytesArray)) != -1) {
                    output.write(bytesArray, 0, bytesRead);
                }
            }

            input.close();
            _ftpObj.completePendingCommand();
            return true;

        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + fullLocalPad + " " + ex.getMessage(), 0);
            return false;
        }
    }

    @Override
    public boolean closeConnection() {
        try {
            if (isLogin) {
                _ftpObj.logout();
                isLogin = false;
            }
            if (isConnected) {
                _ftpObj.disconnect();
                isConnected = false;
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean checkConnectionisAlive() {
        return isConnected && isLogin;
    }

    @Override
    public Object getConnectionObject() {
        return _ftpObj;
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
                FTPFile[] fileListTemp = _ftpObj.listFiles(object.getPath());
                for (FTPFile each : fileListTemp) {
                    RemoteFileObject objectTemp = null;
                    if (each.isDirectory()) {
                        objectTemp = new RemoteFileObject(FileInfoEnum.DIRECTORY);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(object.getPath());
                        directorylist.add(objectTemp);
                    } else if (each.isFile()) {
                        try {
                            objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                            objectTemp.setFileName(each.getName());
                            objectTemp.setAbsolutePath(object.getPath());
                            objectTemp.setFileSize(each.getSize());
                            objectTemp.setFileType();
                            Calendar cal = each.getTimestamp();
                            if (cal != null) {
                                objectTemp.setDate(cal.getTime());
                                willReturnObject.add(objectTemp);
                            }
                        } catch (NullPointerException ex) {
                            if (ex.getMessage().toLowerCase().contains("permission")) {
                                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
                            } else {
                                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
                            }
                        }
                    }
                }
                object = null;
                fileListTemp = null;
            }

        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 0);
            ex.printStackTrace();
            return null;
        }
        return willReturnObject;
    }

    @Override
    public void readAllFilesWalkingPathWithListener(FileListener listener, String remotePath) {
        // List<RemoteFileObject> willReturnObject = new ArrayList<>();
//        System.out.println("Walking Path Listener Opened");
        Queue<RemoteFileObject> directorylist = new LinkedBlockingQueue<>();
        RemoteFileObject object = null;
        object = new RemoteFileObject(FileInfoEnum.DIRECTORY);
        object.setDirectPath(remotePath);
        directorylist.add(object);
        try {
            while (!directorylist.isEmpty()) {
                object = directorylist.poll();
                FTPFile[] fileListTemp = _ftpObj.listFiles(object.getPath());
                for (FTPFile each : fileListTemp) {
                    RemoteFileObject objectTemp = null;
                    if (each.isDirectory()) {
                        objectTemp = new RemoteFileObject(FileInfoEnum.DIRECTORY);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(object.getPath());
                        directorylist.add(objectTemp);
                    } else if (each.isFile()) {
                        try {
                            objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                            objectTemp.setFileName(each.getName());
                            objectTemp.setAbsolutePath(object.getPath());
                            objectTemp.setFileSize(each.getSize());
                            objectTemp.setFileType();
                            objectTemp.setDate(each.getTimestamp().getTime());
                            listener.handleRemoteFile(objectTemp);
                        } catch (NullPointerException ex) {
                            if (ex.getMessage().toLowerCase().contains("permission")) {
                                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
                            } else {
                                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
                            }
                        }
                    }
                }
                object = null;
                fileListTemp = null;
            }

        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 0);
            //    return null;
        }
    }

    @Override
    public void readAllFilesInCurrentPathWithListener(FileListener listener, String remotePath) {

        try {
            FTPFile[] fileListTemp = _ftpObj.listFiles(remotePath);
            for (FTPFile each : fileListTemp) {
                RemoteFileObject objectTemp = null;
                if (each.isFile()) {
                    try {
                        objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(remotePath);
                        objectTemp.setFileSize(each.getSize());
                        objectTemp.setFileType();
                        objectTemp.setDate(each.getTimestamp().getTime());
                        listener.handleRemoteFile(objectTemp);
                    } catch (NullPointerException ex) {
                        if (ex.getMessage().toLowerCase().contains("permission")) {
                            CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
                        } else {
                            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 0);
        }

    }

    @Override
    public List<RemoteFileObject> readAllFilesInCurrentPath(String remotePath) {
//        System.out.println("Entered Method remote path " + remotePath);
        List<RemoteFileObject> willReturnObject = new ArrayList<>();
        try {
            FTPFile[] fileListTemp = _ftpObj.listFiles(remotePath);
//            System.out.println("List size : " + fileListTemp.length);
            for (FTPFile each : fileListTemp) {
                try {
                    RemoteFileObject objectTemp = null;
                    if (each.isFile()) {

                        objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                        //  System.out.println(each);
                        objectTemp.setFileName(each.getName());
                        objectTemp.setAbsolutePath(remotePath);
                        objectTemp.setFileSize(each.getSize());
                        objectTemp.setFileType();
                        objectTemp.setDate(each.getTimestamp().getTime());
                        willReturnObject.add(objectTemp);
                    }
                } catch (NullPointerException ex) {
                    if (ex.getMessage().toLowerCase().contains("permission")) {
                        CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
                    } else {
                        CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
                    }
                }
            }
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 0);
            ex.printStackTrace();
            return null;
        }
        return willReturnObject;
    }

    public void changeFtpConfig(String configKey) {
        _ftpObj.configure(new FTPClientConfig(configKey));
    }

    @Override
    public boolean uploadFile(InputStream input, String fileName, String remotePath) {
        try {
            OutputStream outStream = _ftpObj.storeFileStream(remotePath + fileName);
            byte[] block = new byte[CURRENT_FILE_BYTE_BUFFER];
            int len = 0;
            while ((len = input.read(block)) != -1) {
                outStream.write(block, 0, len);
                outStream.flush();
            }
            outStream.close();
            input.close();
            return true;
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Upload failed to " + _ipAdress + ":" + remotePath + fileName + " " + ex.getMessage(), 0);
            return false;
        }
    }

    @Override
    public boolean uploadFile(File input, String remotePath) {
        try {
            InputStream inputStream = new FileInputStream(input);
            OutputStream outStream = _ftpObj.storeFileStream(remotePath + input.getName());
            byte[] block = new byte[CURRENT_FILE_BYTE_BUFFER];
            int len = 0;
            while ((len = inputStream.read(block)) != -1) {
                outStream.write(block, 0, len);
                outStream.flush();
            }
            outStream.close();
            _ftpObj.completePendingCommand();
            inputStream.close();
            return true;
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Upload failed to " + _ipAdress + ":" + remotePath + input.getName() + " " + ex.getMessage(), 0);
            return false;
        }
    }

    @Override
    public boolean downloadFileWithTtgOutputStreamForXml(String fullRemotePath, ModifiedOutputStream output) {
        InputStream input = null;
        try {
            input = _ftpObj.retrieveFileStream(fullRemotePath);
            int returnCode = _ftpObj.getReplyCode();
            if (input == null || returnCode == 550) {
                return false;
            }

            byte[] bytesArray = new byte[CURRENT_FILE_BYTE_BUFFER];
            int bytesRead = 0;
            while ((bytesRead = input.read(bytesArray)) != -1) {
                output.checkXmlAndWrite(bytesArray, 0, bytesRead);
            }
            output.close();
            input.close();
            _ftpObj.completePendingCommand();
            return true;
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 2);
            try {
                output.close();
            } catch (IOException ex1) {
                CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + ex1.getMessage(), 0);
            }
            return false;
        }
    }

    @Override
    public RemoteFileObject getRemoteFileInfo(String fullRemotePath) {
        RemoteFileObject objectTemp = null;
        try {
            FTPFile each = _ftpObj.listFiles(fullRemotePath)[0];
            if (each.isFile()) {
                objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                objectTemp.setFileName(each.getName());
                objectTemp.setFileSize(each.getSize());
                objectTemp.setDate(each.getTimestamp().getTime());
            }
        } catch (NullPointerException | IOException ex) {
            CommonLibrary.errorLogger("**** getRemoteFileInfo failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 1);
        }
        return objectTemp;
    }
}
