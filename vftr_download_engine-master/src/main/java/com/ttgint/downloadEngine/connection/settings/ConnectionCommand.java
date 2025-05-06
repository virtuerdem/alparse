/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.settings;

import com.ttgint.downloadEngine.connection.exception.ConnectionException;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.factory.ModifiedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author EnesTerzi
 */
public interface ConnectionCommand {

    Object getConnectionObject();

    boolean getConnection();
 
    RemoteFileObject getRemoteFileInfo(String fullRemotePath);

    boolean downloadFile(String fullLocalPad, String fullRemotePath);
    
    boolean downloadFileWithTtgOutputStreamForXml(String fullRemotePath,ModifiedOutputStream output);

    boolean closeConnection();

    boolean checkConnectionisAlive();

    boolean uploadFile(InputStream input, String fileName, String remotePath);

    boolean uploadFile(File input, String remotePath);

    List<RemoteFileObject> readAllFilesWalkinPath(String remotePath);

    List<RemoteFileObject> readAllFilesInCurrentPath(String remotePath);

    List<RemoteFileObject> readAllFilesUsingAnotherLib(String remotePath, Connection con, ConnectionLibs lib) throws ConnectionException;
    
    void readAllFilesWalkingPathWithListener(FileListener listener,String remotePath);
    
    void readAllFilesInCurrentPathWithListener(FileListener listener,String remotePath);

}
