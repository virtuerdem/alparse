/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.connection.settings.FileInfoEnum;
import com.ttgint.downloadEngine.connection.settings.FileListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.SftpClientFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

/**
 *
 * @author EnesTerzi
 */
public class SftpConnectionApacheLib extends Connection {

    private ChannelSftp command;
    private Session session;

    protected SftpConnectionApacheLib(String _ipAdress, String _userName, String _passWord, Integer _port) {
        super(_ipAdress, _userName, _passWord, _port);
    }

    @Override
    public boolean getConnection() {
        if (command != null) {
            closeConnection();
        }
        FileSystemOptions fso = new FileSystemOptions();
        try {
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fso, "no");
            SftpFileSystemConfigBuilder.getInstance().setTimeout(fso, 10000);
            SftpFileSystemConfigBuilder.getInstance().setPreferredAuthentications(fso, "publickey,keyboard-interactive,password");

            session = SftpClientFactory.createConnection(_ipAdress, _port, _userName.toCharArray(), _passWord.toCharArray(), fso);
            Channel channel = session.openChannel("sftp");
            command = (ChannelSftp) channel;
            channel.connect();
            System.out.println("Connected to " + _ipAdress + " via sFTP");
            return true;

        } catch (FileSystemException | JSchException e) {
            //System.out.println("Connection Failed to " + _ipAdress + ". " + e.getMessage());
            CommonLibrary.errorLogger("**** Connection Failed to " + _ipAdress + " " + e.getMessage() + " " + e.toString(), 2);
            return false;
        }
    }

    @Override
    public boolean downloadFile(String fullLocalPad, String fullRemotePath) {
        File file = new File(fullLocalPad);
        InputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = command.get(fullRemotePath);
            if (input == null) {
                return false;
            }
        } catch (SftpException ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + fullLocalPad + " " + ex.getMessage(), 0);
            if (file.exists()) {
                file.delete();
            }
            return false;
        }
        try {
            output = new BufferedOutputStream(new FileOutputStream(file));
            byte[] bytesArray = new byte[CURRENT_FILE_BYTE_BUFFER];
            int bytesRead = -1;
            while ((bytesRead = input.read(bytesArray)) != -1) {
                output.write(bytesArray, 0, bytesRead);
                output.flush();
            }

            output.close();
            input.close();
            return true;

        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + fullLocalPad + " " + ex.getMessage(), 0);
            if (file.exists()) {
                file.delete();
            }
            return false;
        }

    }

    @Override
    public boolean closeConnection() {
        if (command != null || session != null) {
            command.exit();
            session.disconnect();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean checkConnectionisAlive() {
        if (command != null) {
            return command.isConnected();
        }
        return false;
    }

    @Override
    public Object getConnectionObject() {
        return command;
    }

    @Override
    public void readAllFilesWalkingPathWithListener(FileListener listener, String remotePath) {
        //     List<RemoteFileObject> willReturnObject = new ArrayList<>();
        Queue<RemoteFileObject> directorylist = new LinkedBlockingQueue<>();
        RemoteFileObject object = null;
        object = new RemoteFileObject(FileInfoEnum.DIRECTORY);
        object.setDirectPath(remotePath);
        directorylist.add(object);
        try {
            while (!directorylist.isEmpty()) {
                object = directorylist.poll();
                List<ChannelSftp.LsEntry> list = command.ls(object.getPath());
                for (ChannelSftp.LsEntry each : list) {
                    if (each.getFilename().equals(".") || each.getFilename().equals("..")) {
                        continue;
                    }
                    RemoteFileObject objectTemp = null;
                    SftpATTRS attributes = each.getAttrs();

                    if (attributes.isDir()) {
                        objectTemp = new RemoteFileObject(FileInfoEnum.DIRECTORY);
                        objectTemp.setFileName(each.getFilename());
                        objectTemp.setAbsolutePath(object.getPath());
                        directorylist.add(objectTemp);
                    } else if (attributes.isReg()) {
                        objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                        objectTemp.setFileName(each.getFilename());
                        objectTemp.setAbsolutePath(object.getPath());
                        objectTemp.setFileSize(attributes.getSize());
                        objectTemp.setDate(attributes.getMtimeString());
                        objectTemp.setFileType();
                        listener.handleRemoteFile(object);
                    }
                }
                object = null;
                list = null;
            }

        } catch (SftpException ex) {
            if (ex.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
            }
            //  ex.printStackTrace();
        }
        //return willReturnObject;
    }

    @Override
    public void readAllFilesInCurrentPathWithListener(FileListener listener, String remotePath) {
        //   List<RemoteFileObject> willReturnObject = new ArrayList<>();
        try {
            List<ChannelSftp.LsEntry> list = command.ls(remotePath);
            for (ChannelSftp.LsEntry each : list) {
                if (each.getFilename().equals(".") || each.getFilename().equals("..")) {
                    continue;
                }
                RemoteFileObject objectTemp = null;
                SftpATTRS attributes = each.getAttrs();
                if (attributes.isReg()) {
                    objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                    objectTemp.setFileName(each.getFilename());
                    objectTemp.setAbsolutePath(remotePath);
                    objectTemp.setFileSize(attributes.getSize());
                    objectTemp.setDate(attributes.getMtimeString());
                    objectTemp.setFileType();
                    listener.handleRemoteFile(objectTemp);
                }
            }
        } catch (SftpException e) {
            if (e.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + e.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + e.getMessage(), 1);
            }
            //   return null;
        }
        //  return willReturnObject;
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
                try {
                    List<ChannelSftp.LsEntry> list = command.ls(object.getPath());
                    for (ChannelSftp.LsEntry each : list) {
                        if (each.getFilename().equals(".") || each.getFilename().equals("..")) {
                            continue;
                        }
                        RemoteFileObject objectTemp = null;
                        SftpATTRS attributes = each.getAttrs();
                        if (attributes.isDir()) {
                            objectTemp = new RemoteFileObject(FileInfoEnum.DIRECTORY);
                            objectTemp.setFileName(each.getFilename());
                            objectTemp.setAbsolutePath(object.getPath());
                            directorylist.add(objectTemp);
                        } else if (attributes.isReg()) {
                            objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                            objectTemp.setFileName(each.getFilename());
                            objectTemp.setAbsolutePath(object.getPath());
                            objectTemp.setFileSize(attributes.getSize());
                            objectTemp.setDate(attributes.getMtimeString());
                            objectTemp.setFileType();
                            willReturnObject.add(objectTemp);
                        } else {
                            objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                            objectTemp.setFileName(each.getFilename());
                            objectTemp.setAbsolutePath(object.getPath());
                            objectTemp.setFileSize(attributes.getSize());
                            objectTemp.setDate(attributes.getMtimeString());
                            objectTemp.setFileType();
                            willReturnObject.add(objectTemp);
                        }
                    }
                    object = null;
                    list = null;
                } catch (SftpException ex) {
                    if (ex.getMessage().toLowerCase().contains("permission")) {
                        CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
                    } else {
                        CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 1);
                    }
                }

            }
        } catch (Exception ex) {
            CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + ex.getMessage(), 2);
            ex.printStackTrace();
        }

        return willReturnObject;
    }

    @Override
    public List<RemoteFileObject> readAllFilesInCurrentPath(String remotePath) {
        List<RemoteFileObject> willReturnObject = new ArrayList<>();
        try {
            List<ChannelSftp.LsEntry> list = command.ls(remotePath);
            for (ChannelSftp.LsEntry each : list) {
                if (each.getFilename().equals(".") || each.getFilename().equals("..")) {
                    continue;
                }
                RemoteFileObject objectTemp = null;
                SftpATTRS attributes = each.getAttrs();
                if (attributes.isReg()) {
                    objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                    objectTemp.setFileName(each.getFilename());
                    objectTemp.setAbsolutePath(remotePath);
                    objectTemp.setFileSize(attributes.getSize());
                    objectTemp.setDate(attributes.getMtimeString());
                    objectTemp.setFileType();
                    willReturnObject.add(objectTemp);
                }
            }
        } catch (SftpException e) {
            if (e.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + remotePath + " " + e.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Read file error to " + _ipAdress + ":" + remotePath + " " + e.getMessage(), 1);
            }
            return null;
        }
        return willReturnObject;
    }

    @Override
    public boolean uploadFile(InputStream input, String fileName, String remotePath) {
        try {
            OutputStream output = command.put(remotePath + fileName);
            byte[] block = new byte[CURRENT_FILE_BYTE_BUFFER];
            int len = 0;
            while ((len = input.read()) != -1) {
                output.write(block, 0, len);
                output.flush();
            }
            output.close();
            input.close();
            return true;
        } catch (SftpException | IOException ex) {
            CommonLibrary.errorLogger("**** Upload failed to " + _ipAdress + ":" + remotePath + fileName + " " + ex.getMessage(), 0);
            return false;
        }

    }

    @Override
    public boolean uploadFile(File input, String remotePath) {
        try {
            FileInputStream inputste = new FileInputStream(input);
            OutputStream output = command.put(remotePath + input.getName());
            byte[] block = new byte[CURRENT_FILE_BYTE_BUFFER];
            int len = 0;
            while ((len = inputste.read()) != -1) {
                output.write(block, 0, len);
                output.flush();
            }
            output.close();
            inputste.close();
            return true;
        } catch (SftpException | IOException ex) {
            CommonLibrary.errorLogger("**** Upload failed to " + _ipAdress + ":" + remotePath + input.getName() + " " + ex.getMessage(), 0);
            return false;
        }
    }

    @Override
    public boolean downloadFileWithTtgOutputStreamForXml(String fullRemotePath, ModifiedOutputStream output) {

        InputStream input = null;

        try {
            input = command.get(fullRemotePath);
            if (input == null) {
                return false;
            }
        } catch (SftpException ex) {
            CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 0);
            return false;
        }
        try {

            byte[] bytesArray = new byte[CURRENT_FILE_BYTE_BUFFER];
            int bytesRead = -1;
            while ((bytesRead = input.read(bytesArray)) != -1) {
                output.checkXmlAndWrite(bytesArray, 0, bytesRead);
                output.flush();
            }

            output.close();
            input.close();
            return true;

        } catch (IOException ex) {
            if (ex.getMessage().toLowerCase().contains("permission")) {
                CommonLibrary.errorLogger("**** Permission denied to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 2);
            } else {
                CommonLibrary.errorLogger("**** Download failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 1);
            }

            return false;
        }
    }

    @Override
    public RemoteFileObject getRemoteFileInfo(String fullRemotePath) {

        RemoteFileObject objectTemp = null;
        try {
            LsEntry each = (LsEntry) command.ls(fullRemotePath).get(0);

            SftpATTRS attributes = each.getAttrs();
            if (attributes.isReg()) {
                objectTemp = new RemoteFileObject(FileInfoEnum.FILE);
                objectTemp.setFileName(each.getFilename());
                objectTemp.setFileSize(attributes.getSize());
                objectTemp.setDate(attributes.getMtimeString());
            }
        } catch (SftpException ex) {
            CommonLibrary.errorLogger("**** GetRemoteFileInfo failed to " + _ipAdress + ":" + fullRemotePath + " " + ex.getMessage(), 1);
        }
        return objectTemp;
    }
}
