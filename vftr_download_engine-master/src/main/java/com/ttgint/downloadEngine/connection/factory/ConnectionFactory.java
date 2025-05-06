/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.connection.settings.ConnectionListener;

/**
 *
 * @author EnesTerzi
 */
public class ConnectionFactory implements ConnectionListener {

    private final static ConnectionFactory instance
            = new ConnectionFactory();

    private ConnectionFactory() {
    }

    public static ConnectionFactory getInstance() {
        return instance;
    }

    public Connection createConnection(ConnectionLibs lib, String ipAdress, String userName, String passWord, int port) {
        Connection con;
        switch (lib) {
            case ApacheLibFTP:
                con = new FtpConnectionApacheLib(ipAdress, userName, passWord, port);
                break;
            case ApacheLibSFTP:
                con = new SftpConnectionApacheLib(ipAdress, userName, passWord, port);
                break;
            case forJLibFTP:
                con = new FtpConnection4jLib(ipAdress, userName, passWord, port);
                break;
            default:
                throw new RuntimeException("Error");

        }
        con.registerListener(instance);
        return con;
    }

    public Connection createConnection(ConnectionInfo info) {
        Connection con;
        switch (info.getLib()) {
            case ApacheLibFTP:
                con = new FtpConnectionApacheLib(info.getHost(), info.getUsername(), info.getPassWord(), info.getPort());
                break;
            case ApacheLibSFTP:
                con = new SftpConnectionApacheLib(info.getHost(), info.getUsername(), info.getPassWord(), info.getPort());
                break;
            case forJLibFTP:
                con = new FtpConnection4jLib(info.getHost(), info.getUsername(), info.getPassWord(), info.getPort());
                break;
            default:
                throw new RuntimeException("Error");
        }
        con.registerListener(instance);
        return con;
    }

    @Override
    public synchronized void downloadStatus(String fileName, boolean downloadStatus) {
        System.out.println(fileName + " " + downloadStatus);
    }

}
