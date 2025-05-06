/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.FileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.hibernate.pojos.SystemDownloadTimes;
import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import java.io.File;
import java.util.Date;
import com.ttgint.downloadEngine.main.DownloadApp;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author EnesTerzi
 */
abstract class TimeBasedDownloadEngine extends AbsDownloadEngine {

    private SystemDownloadTimes dbDownloadTimeLog;

    private Date lastDateFromdb;
    private Date lastDateFromRemote;

    public TimeBasedDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    /*
    Her ip icin db den last download data otomatik olarak cekilir ve lastDateFromdb field ina atanir.Bu field extend eden 
    class getLastDateFromdb metoduyla cagirabilir. Bu sayade her sistem icin indirilen en son saati surekli cagirmaya gerek
    yoktur.
     */
    public void lastTimeFromDb() {
        Session ses = com.ttgint.downloadEngine.hibernate.utility.HibernateUtility.getSessionFactory().openSession();

        Query que = ses.getNamedQuery("getSystemMaxDownloadTime");
        que.setParameter("systemId", DownloadApp.currentProgressObject.getSystemId());
        que.setParameter("ipId", getServerInfo().getConnectionId().toString());
        try {
            dbDownloadTimeLog = (SystemDownloadTimes) que.list().get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            System.out.println("Creating new date");
            dbDownloadTimeLog = new SystemDownloadTimes();
            dbDownloadTimeLog.setIpId(getServerInfo().getConnectionId().toString());
            dbDownloadTimeLog.setLastDownloadDate(new Date());
            dbDownloadTimeLog.setSystemId(DownloadApp.currentProgressObject.getSystemId());

        }
        lastDateFromdb = dbDownloadTimeLog.getLastDownloadDate();
        lastDateFromRemote = dbDownloadTimeLog.getLastDownloadDate();
        ses.close();
    }

    @Override
    public void requestStop() {
        super.requestStop();
    }

    @Override
    public void examineLocalFile(File file) {
        super.examineLocalFile(file);
    }

    /*
    remote file dan cekilen her file getLastDateFromdb metoduyla gelen field la karsilastirilir.Eger remote file date bu
    tarihten buyuk ise dosya indirilir. Indirilen remote file bu metoda gonderilir. Bunun yapilmasindaki amac 
    olusan en son file bulup db deki date i guncellemektir. Herahngi bir degisiklige gerek yoktur indirilen remotefile i 
    bu metoda gondermek yeterlidir.
     */
    @Override
    public void examineRemoteFile(FileObject fileObj) {
        if (lastDateFromRemote == null) {
            lastDateFromRemote = fileObj.getDate();
            dbDownloadTimeLog.setLastDownloadDate(fileObj.getDate());
            super.examineRemoteFile(fileObj);
            return;
        }
        if (fileObj.getDate().after(lastDateFromRemote)) {
            lastDateFromRemote = fileObj.getDate();
            dbDownloadTimeLog.setLastDownloadDate(fileObj.getDate());
        }
        super.examineRemoteFile(fileObj);

    }

    public Date getLastDateFromdb() {
        return lastDateFromdb;
    }

    @Override
    public abstract void setConnectionInfoLib(ConnectionInfo info);

    public abstract void onDownload(Connection con, ServerIpList connectionInfo);

    @Override
    public abstract void afterFinishForCurrentThread(Connection con);

    @Override
    protected void setCommitSize(int size) {
        super.setCommitSize(size);
    }

    private void updateLastTime() {
        if (lastDateFromRemote.after(lastDateFromdb)) {
            Session ses = HibernateUtility.getSessionFactory().openSession();
            Transaction trx = ses.beginTransaction();
            try {
                ses.saveOrUpdate(dbDownloadTimeLog);
                trx.commit();
            } catch (Exception e) {
                //e.printStackTrace();
                trx.rollback();
            } finally {
                ses.close();
            }
        } else {
            System.out.println(eachIp.getIp() + ":" + eachIp.getRemotePath() + " Nothing downloaded , date updating is not neccesary!");
        }
    }

    @Override
    public void run() {
        lastTimeFromDb();
        setConnectionInfoLib(getConnectionInfoObject());
        setRemoteConnectionConnection();
        onDownload(getRemoteConnection(), getServerInfo());
        afterFinishForCurrentThread(getRemoteConnection());
        updateLastTime();
        super.run();
    }

}
