package com.secura.dnft.entity;


import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "secura_worklist")
public class Worklist {

    @Id
    @Column(name = "worklist_task_id")
    private String worklistTaskId;

    @Column(name = "worklists_type")
    private String worklistsType;

    @Column(name = "status")
    private String status;
    
    @Column(name = "creat_ts")
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usr_id")
    private String lstUpdtUsrId;

    public Worklist() {}


    public String getWorklistTaskId() {
        return worklistTaskId;
    }

    public void setWorklistTaskId(String worklistTaskId) {
        this.worklistTaskId = worklistTaskId;
    }

    public String getWorklistsType() {
        return worklistsType;
    }

    public void setWorklistsType(String worklistsType) {
        this.worklistsType = worklistsType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


	public LocalDateTime getCreatTs() {
		return creatTs;
	}


	public void setCreatTs(LocalDateTime creatTs) {
		this.creatTs = creatTs;
	}


	public String getCreatUsrId() {
		return creatUsrId;
	}


	public void setCreatUsrId(String creatUsrId) {
		this.creatUsrId = creatUsrId;
	}


	public LocalDateTime getLstUpdtTs() {
		return lstUpdtTs;
	}


	public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
		this.lstUpdtTs = lstUpdtTs;
	}


	public String getLstUpdtUsrId() {
		return lstUpdtUsrId;
	}


	public void setLstUpdtUsrId(String lstUpdtUsrId) {
		this.lstUpdtUsrId = lstUpdtUsrId;
	}
}