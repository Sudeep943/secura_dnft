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
        
    @Column(name = "aprmt_id")
    private String aprmtId;
    
    @Column(name = "worklists_type")
    private String worklistsType;

    @Column(name = "status")
    private String status;
    
    @Column(name = "worklists_assign_flow", columnDefinition = "TEXT")
    private String worklistsAssignFlow;
    
    @Column(name = "short_remark")
    private String shortRemark;
    
    @Column(name = "creat_ts")
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usr_id")
    private String lstUpdtUsrId;
    
    @Column(name = "refference_id")
    private String refferenceID;

    public String getWorklistsAssignFlow() {
		return worklistsAssignFlow;
	}


	public String getRefferenceID() {
		return refferenceID;
	}


	public void setRefferenceID(String refferenceID) {
		this.refferenceID = refferenceID;
	}


	public void setWorklistsAssignFlow(String worklistsAssignFlow) {
		this.worklistsAssignFlow = worklistsAssignFlow;
	}


	public String getShortRemark() {
		return shortRemark;
	}


	public void setShortRemark(String shortRemark) {
		this.shortRemark = shortRemark;
	}


	public Worklist() {}


    public String getAprmtId() {
		return aprmtId;
	}


	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}


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
