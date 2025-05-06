/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.hibernate.pojos;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.NamedQueries;
import javax.persistence.Table;

/**
 *
 * @author ibrahimegerci
 */
@Entity
@Table(name = "KPI_LIST")
@NamedQueries(
        {
            @NamedQuery(name = "allActiveKpiList", query = "from KpiList s where s.isActive = 1 and s.operatorName = :operatorName and s.systemType = :systemType and s.measType = :measType "),
            @NamedQuery(name = "allActiveKpiListNe", query = "from KpiList s where s.isActive = 1 and s.operatorName = :operatorName and s.systemType = :systemType and s.measType = :measType and s.neType = :neType ")
        })

public class KpiList implements java.io.Serializable {

    @Id
    @Column(name = "SEQ")
    private int seq;

    @Column(name = "KPI_NAME")
    private String kpiName;

    @Column(name = "OPERATOR_NAME")
    private String operatorName;

    @Column(name = "SYSTEM_TYPE")
    private String systemType;

    @Column(name = "MEAS_TYPE")
    private String measType;

    @Column(name = "IS_ACTIVE")
    private int isActive;

    @Column(name = "ACTIVATION_DATE")
    private Date activationDate;

    @Column(name = "NE_TYPE")
    private String neType;

    @Column(name = "KPI_GROUP_ID")
    private int kpiGroupId;

    @Column(name = "KPI_GROUP")
    private String kpiGroup;

    @Column(name = "KPI_GROUP_NAME")
    private String kpiGroupName;

    public KpiList() {
    }

    public KpiList(int seq, String kpiName, String operatorName, String systemType, String measType, int isActive, Date activationDate, String neType, int kpiGroupId, String kpiGroup, String kpiGroupName) {
        this.seq = seq;
        this.kpiName = kpiName;
        this.operatorName = operatorName;
        this.systemType = systemType;
        this.measType = measType;
        this.isActive = isActive;
        this.activationDate = activationDate;
        this.neType = neType;
        this.kpiGroup = kpiGroup;
        this.kpiGroupId = kpiGroupId;
        this.kpiGroupName = kpiGroupName;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getKpiName() {
        return kpiName;
    }

    public void setKpiName(String kpiName) {
        this.kpiName = kpiName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public Date getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(Date activationDate) {
        this.activationDate = activationDate;
    }

    public String getNeType() {
        return neType;
    }

    public void setNeType(String neType) {
        this.neType = neType;
    }

    public int getKpiGroupId() {
        return kpiGroupId;
    }

    public void setKpiGroupId(int kpiGroupId) {
        this.kpiGroupId = kpiGroupId;
    }

    public String getKpiGroup() {
        return kpiGroup;
    }

    public void setKpiGroup(String kpiGroup) {
        this.kpiGroup = kpiGroup;
    }

    public String getKpiGroupName() {
        return kpiGroupName;
    }

    public void setKpiGroupName(String kpiGroupName) {
        this.kpiGroupName = kpiGroupName;
    }

}
