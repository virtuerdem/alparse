/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.hibernate.pojos;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 *
 * @author EnesTerzi
 */
@Entity
@Table(name = "DOWNLOAD_PROCESS_LOG")
public class DownloadProcessLog implements Serializable {

    @Id
    @SequenceGenerator(name = "log_generator", schema = "NORTHI_PARSER_SETTINGS", sequenceName = "SEQ_PPLOG")
    @GeneratedValue(generator = "log_generator", strategy = GenerationType.SEQUENCE)
    @Column(name = "LOG_ID")
    private Integer logId;

    @Column(name = "SYSTEM_TYPE", nullable = false)
    private String systemType;

    @Column(name = "MEAS_TYPE", nullable = false)
    private String measType;

    @Column(name = "OPERATOR_NAME", nullable = false)
    private String operatorName;

    @Column(name = "PROCESS_START_TIME")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date processStartTime;

    @Column(name = "PROCESS_STOP_TIME")
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date processStopTime;

    @Column(name = "PID_NO", nullable = false)
    private Long pidNo;

    @Column(name = "PROGRESS_STATUS", nullable = false)
    private String progressStatus;

    @Column(name = "TOTAL_DOWNLOAD_SIZE")
    private long totalDownloadSize;

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getMeasType() {
        return measType;
    }

    public void setMeasType(String measType) {
        this.measType = measType;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Date getProcessStartTime() {
        return processStartTime;
    }

    public void setProcessStartTime(Date processStartTime) {
        this.processStartTime = processStartTime;
    }

    public Date getProcessStopTime() {
        return processStopTime;
    }

    public void setProcessStopTime(Date processStopTime) {
        this.processStopTime = processStopTime;
    }

    public Long getPidNo() {
        return pidNo;
    }

    public void setPidNo(Long pidNo) {
        this.pidNo = pidNo;
    }

    public String getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    public long getTotalDownloadSize() {
        return totalDownloadSize;
    }

    public void setTotalDownloadSize(long totalDownloadSize) {
        this.totalDownloadSize = totalDownloadSize;
    }

}
