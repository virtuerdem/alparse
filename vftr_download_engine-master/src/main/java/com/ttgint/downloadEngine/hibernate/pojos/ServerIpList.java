package com.ttgint.downloadEngine.hibernate.pojos;
// Generated Oct 30, 2014 4:25:05 PM by Hibernate Tools 4.3.1

import java.util.HashMap;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "SERVER_IP_LIST")
@NamedQueries({
    @NamedQuery(name = "getIpList",
            query = "from ServerIpList s where s.systemType = :systemType and s.operatorName = :operatorName "
            + " and s.isActive=1 and s.dataFileType = :measType order by s.connectionId"),
    @NamedQuery(name = "getIpListCheck",
            query = "from ServerIpList s where s.systemType = :systemType and s.operatorName = :operatorName "
            + " and s.isActive=1 order by s.connectionId"),
    @NamedQuery(name = "u2000ip",
            query = "from ServerIpList s where s.systemType = :systemType and s.operatorName = :operatorName "
            + " and s.isActive=0 and s.dataFileType = :measType and s.elementManager like :elementManager")
})
public class ServerIpList implements java.io.Serializable {

    @Id
    @Column(name = "CONNECTION_ID")
    private Integer connectionId;

    /*
     for entering extra property
     */
    @Transient
    private final HashMap<String, String> property
            = new HashMap<>();

    @Column(name = "IP")
    private String ip;

    @Column(name = "CON_PROTOCOL_TYPE")
    private String conProtocolType;

    @Column(name = "UNAME")
    private String uname;

    @Column(name = "UPASS")
    private String upass;

    @Column(name = "PORT")
    private Integer port;

    @Column(name = "SYSTEM_TYPE")
    private String systemType;

    @Column(name = "IS_ACTIVE")
    private Integer isActive;

    @Column(name = "ELEMENT_MANAGER")
    private String elementManager;

    @Column(name = "ELEMENT_MANAGER_NAME")
    private String elementManagerName;

    @Column(name = "DATA_FILE_TYPE")
    private String dataFileType;

    @Column(name = "REMOTE_PATH")
    private String remotePath;

    @Column(name = "OPERATOR_NAME")
    private String operatorName;

    public ServerIpList() {
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getConProtocolType() {
        return this.conProtocolType;
    }

    public void setConProtocolType(String conProtocolType) {
        this.conProtocolType = conProtocolType;
    }

    public String getUname() {
        return this.uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUpass() {
        return this.upass;
    }

    public void setUpass(String upass) {
        this.upass = upass;
    }

    public String getSystemType() {
        return this.systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public Integer getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Integer connectionId) {
        this.connectionId = connectionId;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getElementManager() {
        return this.elementManager;
    }

    public void setElementManager(String elementManager) {
        this.elementManager = elementManager;
    }

    public String getElementManagerName() {
        return elementManagerName;
    }

    public void setElementManagerName(String elementManagerName) {
        this.elementManagerName = elementManagerName;
    }

    public String getDataFileType() {
        return this.dataFileType;
    }

    public void setDataFileType(String dataFileType) {
        this.dataFileType = dataFileType;
    }

    public String getRemotePath() {
        return this.remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public void putProperty(String key, String value) {
        property.put(key, value);
    }

    public String getProperty(String key) {
        return property.get(key);
    }

}
