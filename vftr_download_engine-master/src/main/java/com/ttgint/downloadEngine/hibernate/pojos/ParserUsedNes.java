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
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 *
 * @author TTGETERZI
 */
@Entity
@Table(name = "PARSER_USED_NES")
@NamedQueries({
    @NamedQuery(name = "hw3gconfig", query = "from ParserUsedNes b where b.isActive=1 and b.neIp=:neIp"),
    @NamedQuery(name = "getNeByIp", query = "from ParserUsedNes a where a.m2000ServerIp=:m2000ServerIp"),
    @NamedQuery(name = "getNeByIpAndSysOpName", query = "select distinct a.neName from ParserUsedNes a where a.isActive = 1 and a.m2000ServerIp=:m2000ServerIp and a.systemType =:systemType and a.operatorName=:operatorName"),
    @NamedQuery(name = "hwcmNeNameQuery", query = "from ParserUsedNes a where a.systemType =:systemType "
            + "and a.operatorName =:operatorName and a.isActive = 1 and a.m2000ServerIpCm =:m2000ServerIpCms "),
    @NamedQuery(name = "hwConf", query = "from ParserUsedNes a where a.isActive=1 and a.systemType =:systemType "
            + "and (a.m2000ServerIpCm=:serverIp or a.m2000ServerIp=:serverIp)"),
    @NamedQuery(name = "getNeBySystem", query = "select distinct a.neName from ParserUsedNes a where a.isActive = 1 and a.systemType =:systemType and a.operatorName=:operatorName")
})
public class ParserUsedNes implements Serializable {

    @Id
    @Column(name = "ELEMENT_ID")
    Integer elementId;
    @Column(name = "NE_NAME")
    String neName;
    @Column(name = "M2000_SERVER_IP")
    String m2000ServerIp;
    @Column(name = "RAW_NE_ID")
    Integer rawNeId;
    @Column(name = "IS_ACTIVE")
    Integer isActive;
    @Column(name = "NE_IP")
    String neIp;
    @Column(name = "SYSTEM_TYPE")
    String systemType;
    @Column(name = "M2000_SERVER_IP_CM")
    String m2000ServerIpCm;
    @Column(name = "OPERATOR_NAME")
    String operatorName;
    @Column(name = "CM_STATE")
    Integer cmState;
    @Column(name = "CM_EXECUTION_TIME")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date cmExecutionTime;

    public Integer getElementId() {
        return elementId;
    }

    public void setElementId(Integer elementId) {
        this.elementId = elementId;
    }

    public String getNeName() {
        return neName;
    }

    public void setNeName(String neName) {
        this.neName = neName;
    }

    public String getM2000ServerIp() {
        return m2000ServerIp;
    }

    public void setM2000ServerIp(String m2000ServerIp) {
        this.m2000ServerIp = m2000ServerIp;
    }

    public Integer getRawNeId() {
        return rawNeId;
    }

    public void setRawNeId(Integer rawNeId) {
        this.rawNeId = rawNeId;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getNeIp() {
        return neIp;
    }

    public void setNeIp(String neIp) {
        this.neIp = neIp;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getM2000ServerIpCm() {
        return m2000ServerIpCm;
    }

    public void setM2000ServerIpCm(String m2000ServerIpCm) {
        this.m2000ServerIpCm = m2000ServerIpCm;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getCmState() {
        return cmState;
    }

    public void setCmState(Integer cmState) {
        this.cmState = cmState;
    }

    public Date getCmExecutionTime() {
        return cmExecutionTime;
    }

    public void setCmExecutionTime(Date cmExecutionTime) {
        this.cmExecutionTime = cmExecutionTime;
    }

}
