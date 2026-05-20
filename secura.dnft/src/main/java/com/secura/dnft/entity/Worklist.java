package com.secura.dnft.entity;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "secura_worklist")
public class Worklist {

    @Id
    @Column(name = "worklist_task_id")
    private String worklistId;
        
    @Column(name = "aprmt_id")
    private String apartmentId;
    
    @Column(name = "worklists_type")
    private String worklistType;

    @Column(name = "status")
    private String status;
    
    @Column(name = "worklists_assign_flow", columnDefinition = "TEXT")
    private String worklistAssignmentFlow;

    @Column(name = "current_assignee")
    private String currentAssignee;
    
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
    private String referenceId;

    public String getWorklistId() {
		return worklistId;
	}


	public void setWorklistId(String worklistId) {
		this.worklistId = worklistId;
	}


	public String getApartmentId() {
		return apartmentId;
	}


	public void setApartmentId(String apartmentId) {
		this.apartmentId = apartmentId;
	}


	public String getWorklistType() {
		return worklistType;
	}


	public void setWorklistType(String worklistType) {
		this.worklistType = worklistType;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public String getWorklistAssignmentFlow() {
		return worklistAssignmentFlow;
	}


	public void setWorklistAssignmentFlow(String worklistAssignmentFlow) {
		this.worklistAssignmentFlow = worklistAssignmentFlow;
	}


	public String getCurrentAssignee() {
		return currentAssignee;
	}


	public void setCurrentAssignee(String currentAssignee) {
		this.currentAssignee = currentAssignee;
	}


	public String getShortRemark() {
		return shortRemark;
	}


	public void setShortRemark(String shortRemark) {
		this.shortRemark = shortRemark;
	}


	public Worklist() {}


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


	public String getReferenceId() {
		return referenceId;
	}


	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}


	@JsonIgnore
    public String getWorklistTaskId() {
        return worklistId;
    }

	@JsonIgnore
    public void setWorklistTaskId(String worklistTaskId) {
        this.worklistId = worklistTaskId;
    }

	@JsonIgnore
    public String getAprmtId() {
		return apartmentId;
	}

	@JsonIgnore
	public void setAprmtId(String aprmtId) {
		this.apartmentId = aprmtId;
	}

	@JsonIgnore
    public String getWorklistsType() {
        return worklistType;
    }

	@JsonIgnore
    public void setWorklistsType(String worklistsType) {
        this.worklistType = worklistsType;
    }

	@JsonIgnore
    public String getWorklistsAssignFlow() {
		return worklistAssignmentFlow;
	}

	@JsonIgnore
	public void setWorklistsAssignFlow(String worklistsAssignFlow) {
		this.worklistAssignmentFlow = worklistsAssignFlow;
	}

	@JsonIgnore
	public String getRefferenceID() {
		return referenceId;
	}

	@JsonIgnore
	public void setRefferenceID(String refferenceID) {
		this.referenceId = refferenceID;
	}
}
