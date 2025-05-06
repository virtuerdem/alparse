/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.connection.exception.ConnectionException;
import com.ttgint.downloadEngine.connection.settings.ConnectionCommand;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.connection.settings.ConnectionListener;
import com.ttgint.downloadEngine.connection.settings.ConnectionSlave;
import java.util.List;

/**
 *
 * @author EnesTerzi
 */
public abstract class Connection
        implements ConnectionCommand, ConnectionSlave {

    protected final String _ipAdress;
    protected final String _userName;
    protected String _passWord;
    protected final Integer _port;
    private ConnectionListener listener;

    protected final int DEFAULT_FILE_BYTE_SIZE = 1024;

    protected int CURRENT_FILE_BYTE_BUFFER;

    {
        CURRENT_FILE_BYTE_BUFFER = DEFAULT_FILE_BYTE_SIZE;
    }
    protected boolean isConnected = false;
    protected boolean isLogin = false;

    protected Connection(String _ipAdress, String _userName, String _passWord, Integer _port) {
        if (_ipAdress == null
                || _userName == null
                || _passWord == null) {
            throw new NullPointerException();
        }
        this._ipAdress = _ipAdress;
        this._userName = _userName;
        this._passWord = _passWord;
        this._port = _port;
    }

    public void setByteBuffer(int size) {
        this.CURRENT_FILE_BYTE_BUFFER = size;
    }

    @Override
    public abstract List<RemoteFileObject> readAllFilesInCurrentPath(String remotePath);

    @Override
    public abstract List<RemoteFileObject> readAllFilesWalkinPath(String remotePath);

    @Override
    public abstract boolean checkConnectionisAlive();

    @Override
    public abstract boolean closeConnection();

    @Override
    public abstract boolean downloadFile(String fullLocalPad, String fullRemotePath);

    @Override
    public abstract boolean getConnection();

    @Override
    public abstract Object getConnectionObject();

    @Override
    public List<RemoteFileObject> readAllFilesUsingAnotherLib(String remotePath, Connection con, ConnectionLibs lib) throws ConnectionException {
        try {
            Connection newConnection = null;
            switch (lib) {
                case ApacheLibFTP:
                    newConnection = new FtpConnectionApacheLib(con.getIpAdress(), con.getUserName(), con.getPassWord(), 21);
                    break;
                case ApacheLibSFTP:
                    newConnection = new SftpConnectionApacheLib(con.getIpAdress(), con.getUserName(), con.getPassWord(), 22);
                    break;
                case forJLibFTP:
                    newConnection = new FtpConnection4jLib(con.getIpAdress(), con.getUserName(), con.getPassWord(), 21);
                    break;
            }
            if (newConnection == null) {
                CommonLibrary.errorLogger("**** Connection error: " + con.getIpAdress() + ":" + remotePath + " " + "Couldnt initilaze new Lib", 2);
                throw new ConnectionException("Couldnt initilaze new Lib;");
            }

            if (newConnection.getConnection() == false) {
                CommonLibrary.errorLogger("**** Connection error: " + con.getIpAdress() + ":" + remotePath + " " + "Couldnt connect Remote ip", 2);
                throw new ConnectionException("Couldn connect Remote ip");
            }
            List<RemoteFileObject> tempList = newConnection.readAllFilesWalkinPath(remotePath);
            newConnection.closeConnection();
            return tempList.isEmpty() ? null : tempList;
        } catch (ConnectionException ce) {
            CommonLibrary.errorLogger("**** Connection error: " + con.getIpAdress() + ":" + remotePath + " " + ce.getMessage(), 2);
            throw ce;
        }
    }

    protected String getIpAdress() {
        return _ipAdress;
    }

    protected String getUserName() {
        return _userName;
    }

    protected String getPassWord() {
        return _passWord;
    }

    protected Integer getPort() {
        return _port;
    }

    @Override
    public void registerListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public ConnectionListener getListener() {
        return listener;
    }

}
