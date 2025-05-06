/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.hibernate.pojos;

import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author EnesTerzi
 */
@Entity
@Table(name = "DOWNLOAD_DATE_HISTORY")

@NamedQueries({
    @NamedQuery(name = "getSystemMaxDownloadTime", query = "from SystemDownloadTimes a where "
            + " a.systemId=:systemId and a.ipId=:ipId "),
    @NamedQuery(name = "getSystemMaxDownloadTimeWithoutIp", query = "from SystemDownloadTimes a where "
            + " a.systemId=:systemId")
})

public class SystemDownloadTimes implements Serializable {

    @Id
    @SequenceGenerator(name = "downloadidgenerator", schema = "NORTHI_PARSER_SETTINGS", sequenceName = "SEQ_DOWNLOAD_ID")
    @GeneratedValue(generator = "downloadidgenerator", strategy = GenerationType.SEQUENCE)
    @Column(name = "DOWNLOAD_ID")
    private Integer downloadId;

    @Column(name = "DOWNLOAD_DATE_LAST")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastDownloadDate;

    @Column(name = "IP_ID")
    private String ipId;

    @Column(name = "SYSTEM_ID")
    private Integer systemId;

    public static SystemDownloadTimes lastTimeFromDb(int systemId, int ipId) {
        Session ses = com.ttgint.downloadEngine.hibernate.utility.HibernateUtility.getSessionFactory().openSession();

        Query que = ses.getNamedQuery("getSystemMaxDownloadTime");
        que.setParameter("systemId", systemId);
        que.setParameter("ipId", ipId);

        SystemDownloadTimes dbDownloadTimeLog = (SystemDownloadTimes) que.list().get(0);
        ses.close();

        return dbDownloadTimeLog;
    }

    public static void updateLastTime(SystemDownloadTimes dbDownloadTimeLog, Date lastDateFromRemote, Date lastDateFromdb) {
        if (lastDateFromRemote.after(lastDateFromdb)) {
            Session ses = HibernateUtility.getSessionFactory().openSession();
            Transaction trx = ses.beginTransaction();
            dbDownloadTimeLog.setLastDownloadDate(lastDateFromRemote);
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
            System.out.println("Nothing downloaded , date updating is not neccesary!");
        }
    }

    public Integer getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(Integer downloadId) {
        this.downloadId = downloadId;
    }

    public Date getLastDownloadDate() {
        return lastDownloadDate;
    }

    public void setLastDownloadDate(Date lastDownloadDate) {
        this.lastDownloadDate = lastDownloadDate;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public Integer getSystemId() {
        return systemId;
    }

    public void setSystemId(Integer systemId) {
        this.systemId = systemId;
    }

}
