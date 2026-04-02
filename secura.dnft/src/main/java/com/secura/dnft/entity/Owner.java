package com.secura.dnft.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "secura_owner")
public class Owner {
	
	@Id
    @Column(name = "ownerid")
    private String ownerId;

	
    @Column(name = "aprmt_id")
    private String aprmt_id;

    @Column(name = "prfl_id")
    private String prflId;

    @Column(name = "flat_no")
    private String flatNo;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "document" , columnDefinition = "TEXT")
    private String document;
    
    
    @Column(name = "creat_ts")
    @CreationTimestamp
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usr_id")
    private String lstUpdtUsrId;
    
    public String getAprmt_id() {
		return aprmt_id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public void setAprmt_id(String aprmt_id) {
		this.aprmt_id = aprmt_id;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public Owner() {}

    // Getters & Setters

    public String getPrflId() { return prflId; }
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

	public void setPrflId(String prflId) { this.prflId = prflId; }

    public String getFlatNo() { return flatNo; }
    public void setFlatNo(String flatNo) { this.flatNo = flatNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}