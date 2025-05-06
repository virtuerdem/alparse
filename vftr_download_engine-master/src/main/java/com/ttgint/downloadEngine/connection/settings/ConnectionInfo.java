/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.settings;

/**
 *
 * @author EnesTerzi
 */
public class ConnectionInfo {

    private final String host;
    private final String Username;
    private final String PassWord;
    private final int port;
    private ConnectionLibs lib;

    public ConnectionInfo(String host, String Username, String PassWord, int port) {
        this.host = host;
        this.Username = Username;
        this.PassWord = PassWord;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return Username;
    }

    public String getPassWord() {
        return PassWord;
    }

    public int getPort() {
        return port;
    }

    public ConnectionLibs getLib() {
        return lib;
    }

    public void setLib(ConnectionLibs lib) {
        this.lib = lib;
    }

}
