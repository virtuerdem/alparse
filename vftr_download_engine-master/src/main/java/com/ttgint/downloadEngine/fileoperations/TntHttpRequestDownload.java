/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngine(measType = "CM", systemType = "ALU-NE", operatorName = "TURKTELEKOM")
public class TntHttpRequestDownload extends DefaultTimeBasedEngine {

    static boolean isStartedRequest = false;

    public TntHttpRequestDownload(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {

        String host = "https://" + connectionInfo.getIp() + ":8443/xmlapi/invoke";
        System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " requests starting host: " + host);

        if (isStartedRequest == false) {
            try {
                ArrayList<File> requestXmlFileList = CommonLibrary.list_AllFilesAsFile(System.getProperty("user.dir") + "/" + "HTTP_XML/");
                for (File requestXmlFile : requestXmlFileList) {
                    if (requestXmlFile.getName().endsWith(".xml")) {
                        try {
                            System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " request prepare host: " + host + " file: " + requestXmlFile.getName());
                            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(new File("nsp.pem")));

                            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            keyStore.load(null, null);
                            keyStore.setCertificateEntry("server", certificate);

                            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                            trustManagerFactory.init(keyStore);

                            SSLContext sslContext = SSLContext.getInstance("SSL");
                            sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());
                            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

                            System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " request sending host: " + host + " file: " + requestXmlFile.getName());
                            HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(host).openConnection();

                            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                            HostnameVerifier allHostsValid = new HostnameVerifier() {
                                public boolean verify(String hostname, SSLSession session) {
                                    return true;
                                }
                            };
                            httpsConnection.setHostnameVerifier(allHostsValid);

                            httpsConnection.setRequestMethod("POST");
                            httpsConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");

                            httpsConnection.setDoOutput(true);
                            DataOutputStream dataOutputStream = new DataOutputStream(httpsConnection.getOutputStream());
                            dataOutputStream.writeBytes(FileUtils.readFileToString(requestXmlFile, StandardCharsets.UTF_8));
                            dataOutputStream.flush();
                            dataOutputStream.close();

                            httpsConnection.connect();
                            System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " request connected host: " + host + " file: " + requestXmlFile.getName() + " responseCode: " + httpsConnection.getResponseCode());
                            try {
                                Thread.sleep(60000 * 5);
                            } catch (InterruptedException ex) {
                            }

                            System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " request done host: " + host + " file: " + requestXmlFile.getName() + " responseCode: " + httpsConnection.getResponseCode());
                            httpsConnection.disconnect();
                        } catch (Exception exception) {
                            System.out.println("> " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm:ss") + " request failed host: " + host + " file: " + requestXmlFile.getName() + " exception: " + exception.toString());
                            exception.printStackTrace();
                            throw exception;
                        }
                    }
                }
                isStartedRequest = true;
            } catch (Exception e) {
            }
        }

        if (con.getConnection()) {
            System.out.println("Reading Files from " + connectionInfo.getIp());
            List<RemoteFileObject> list = con.readAllFilesWalkinPath(connectionInfo.getRemotePath());
            System.out.println("List Size from " + connectionInfo.getIp() + " : " + list.size());

            if (list != null) {
                for (RemoteFileObject each : list) {
                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());
                    if (!downloadStatus) {
                        System.out.println("download failed : " + each.getFileName());
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
                    Date fileDate = Date.from(instant);

                    FileHousekeep fileHousekeepObj = new FileHousekeep();
                    fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                    fileHousekeepObj.setDownloadTryCount(1);
                    fileHousekeepObj.setFileDate(fileDate);
                    fileHousekeepObj.setFileName(each.getFileName());
                    fileHousekeepObj.setFileSize(each.getFileSize());
                    fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                    fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                    fileHousekeepObj.setFileCreatedDate(each.getDate());

                    dbOperationNonExistFileObject(fileHousekeepObj, each);
                }
            }
        }
    }
}
