package com.ttgint.downloadEngine.hibernate.pojos;
// Generated Oct 30, 2014 3:56:54 PM by Hibernate Tools 4.3.1

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "PARSER_START_STOP")
@NamedQueries({
    @NamedQuery(
            name = "findByMachineIp",
            query = "from ParserStartStop s where s.workingIp = :ipPar"
    ),
    @NamedQuery(name = "findByOther", query = "from ParserStartStop s where s.systemType=:systemType "
            + " and s.measType=:measType and s.operatorName=:operatorName")
})

public class ParserStartStop implements Serializable {

    @Id
    @Column(name = "SYSTEM_ID")
    private Integer systemId;
    @Column(name = "SYSTEM_TYPE")
    private String systemType;
    @Column(name = "MEAS_TYPE")
    private String measType;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "OPERATOR_NAME")
    private String operatorName;
    @Column(name = "WORKING_IP")
    private String workingIp;
    @Column(name = "IS_ACTIVE")
    private Integer isSystemActive;
    @Column(name = "IS_PARSER_ACTIVE")
    private Integer isParserActive;
    @Column(name = "IS_DOWNLOAD_ACTIVE")
    private Integer isDownloadActive;
    @Column(name = "SQLLDR_FLAG")
    private Integer sqlldrFlag;
    @Column(name = "PROCEDURE_FLAG")
    private Integer procedureFlag;
    @Column(name = "LOADER_FLAG")
    private Integer loaderFlag;
    @Column(name = "PARSER_THREAD")
    private Integer parserThread;
    @Column(name = "START_TIME")
    private String startTime;
    @Column(name = "DOWNLOAD_STATUS")
    private String downloadStatus;
    @Column(name = "OPERATOR_SHORT_CODE")
    private String operatorShortCode;

    public String getOperatorShortCode() {
        return operatorShortCode;
    }

    public void setOperatorShortCode(String operatorShortCode) {
        this.operatorShortCode = operatorShortCode;
    }

    public String getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(String downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public ParserStartStop() {
    }

    public ParserStartStop(Integer systemId) {
        this.systemId = systemId;
    }

    public boolean getIsSystemActive() {
        return isSystemActive == 1;
    }

    public Integer getSystemId() {
        return systemId;
    }

    public String getSystemType() {
        return systemType;
    }

    public String getMeasType() {
        return measType;
    }

    public String getStatus() {
        return status;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getWorkingIp() {
        return workingIp;
    }

    public boolean getIsParserActive() {
        return isParserActive == 1;
    }

    public boolean getIsDownloadActive() {
        return isDownloadActive == 1;
    }

    public Integer getSqlldrFlag() {
        return sqlldrFlag;
    }

    public Integer getProcedureFlag() {
        return procedureFlag;
    }

    public Integer getLoaderFlag() {
        return loaderFlag;
    }

    public String getStartTime() {
        return startTime;
    }

    public Integer getParserThread() {
        return parserThread;
    }
    
    @Override
    public String toString() {
        return "systemType=" + systemType + ", measType=" + measType + ", operatorName=" + operatorName;
    }

}
