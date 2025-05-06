package com.ttgint.downloadEngine.hibernate.pojos;
// Generated Oct 30, 2014 3:56:54 PM by Hibernate Tools 4.3.1

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
import javax.persistence.TemporalType;

@Entity
@Table(name = "FILE_HOUSEKEEP")
@NamedQueries(
        {
            @NamedQuery(name = "standartDownloadQuery", query = "from FileHousekeep b where b.downloaded = 0 and b.fileDate < :fileDate "
                    + " and b.operatorName = :operatorName and b.systemType = :systemType and b.connection = :connection "
                    + " and b.fileDate > sysdate-14 and b.downloadTryCount < 10 "),

            @NamedQuery(name = "standartDownloadQueryHW4G", query = "from FileHousekeep b where b.downloaded = 0 and b.fileDate < :fileDate "
                    + " and b.operatorName = :operatorName and b.systemType = :systemType and b.connection = :connection "
                    + " and b.fileDate > sysdate-5 and b.downloadTryCount < 3 "),

            @NamedQuery(name = "standartDownloadFileCheck", query = "from FileHousekeep b where b.downloaded = 1 and b.fileDate < :fileDate "
                    + " and b.operatorName = :operatorName and b.systemType like CONCAT('%',:systemType) and b.measType = :measType "
                    + " and b.fileDate >= trunc(sysdate-2/24) and b.connection = :connection "),

            @NamedQuery(name = "standartDownloadQueryTimeBased", query = "from FileHousekeep b where b.downloaded = 0 "
                    + " and b.systemType = :systemType and b.connection = :connection "
                    + " and b.fileDate > sysdate-14 and b.downloadTryCount < 10 "),

            @NamedQuery(name = "standartDownloadQueryTimeBasedPostgreSql", query = "from FileHousekeep b where b.downloaded = 0 "
                    + " and b.systemType = :systemType and b.connection = :connection "
                    + " and b.fileDate > current_date-14 and b.downloadTryCount < 10 ")
        })

public class FileHousekeep implements java.io.Serializable {

    @Id
    @SequenceGenerator(schema = "NORTHI_PARSER_SETTINGS", sequenceName = "SEQ_FILE_ID", name = "seq_file_id", allocationSize = 1)
    @GeneratedValue(generator = "seq_file_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "FILE_ID")
    Long fileId;

    @Column(name = "FILE_NAME")
    String fileName;

    @Column(name = "DOWNLOADED")
    Integer downloaded;

    @Column(name = "DOWNLOAD_TRY_COUNT")
    Integer downloadTryCount;

    @Column(name = "SYSTEM_TYPE")
    String systemType;

    @Column(name = "FILE_SIZE")
    Long fileSize;

    @Column(name = "OPERATOR_NAME")
    String operatorName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FILE_DATE")
    Date fileDate;

    @Column(name = "CONNECTION_ID")
    Integer connection;

    @Column(name = "FILE_CREATED_DATE")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date fileCreatedDate;

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "FILE_DOWNLOAD_DATE")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date fileDownloadDate;

    @Column(name = "MEAS_TYPE")
    String measType;

    public Date getFileCreatedDate() {
        return fileCreatedDate;
    }

    public void setFileCreatedDate(Date fileCreatedDate) {
        this.fileCreatedDate = fileCreatedDate;
    }

    public Integer getConnectionId() {
        return connection;
    }

    public void setConnectionId(Integer connectionId) {
        this.connection = connectionId;
    }

    public FileHousekeep() {
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(Integer downloaded) {
        this.downloaded = downloaded;
    }

    public Integer getDownloadTryCount() {
        return downloadTryCount;
    }

    public void setDownloadTryCount(Integer downloadTryCount) {
        this.downloadTryCount = downloadTryCount;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getFileDownloadDate() {
        return fileDownloadDate;
    }

    public void setFileDownloadDate(Date fileDownloadDate) {
        this.fileDownloadDate = fileDownloadDate;
    }

    public String getMeasType() {
        return measType;
    }

    public void setMeasType(String measType) {
        this.measType = measType;
    }

    public FileHousekeep(String fileName, Integer downloaded,
            Integer downloadTryCount, String systemType, Long fileSize,
            String operatorName, Date fileDate, Integer connection, Date fileCreatedDate) {
        this.connection = connection;
        this.fileName = fileName;
        this.downloaded = downloaded;
        this.downloadTryCount = downloadTryCount;
        this.systemType = systemType;
        this.fileSize = fileSize;
        this.operatorName = operatorName;
        this.fileDate = fileDate;
        this.fileCreatedDate = fileCreatedDate;
    }

}
