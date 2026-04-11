package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_flat")
public class Flat {

	@Column(name = "aprmnt_id")
	private String aprmntId;

	@Id
	@Column(name = "flat_no", nullable = false)
	private String flatNo;

	@Column(name = "flat_owner_list", columnDefinition = "TEXT")
	private String flatOwnerList;

	@Column(name = "flat_tower")
	private String flatTower;

	@Column(name = "flat_block")
	private String flatBlock;

	@Column(name = "flat_possn_date")
	private LocalDateTime flatPossnDate;

	@Column(name = "flat_owner_type")
	private String flatOwnerType;

	@Column(name = "flat_area")
	private String flatArea;

	@Column(name = "flat_pndng_paymnt_lst", columnDefinition = "TEXT")
	private String flatPndngPaymntLst;

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

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getFlatNo() {
		return flatNo;
	}

	public void setFlatNo(String flatNo) {
		this.flatNo = flatNo;
	}

	public String getFlatOwnerList() {
		return flatOwnerList;
	}

	public void setFlatOwnerList(String flatOwnerList) {
		this.flatOwnerList = flatOwnerList;
	}

	public String getFlatTower() {
		return flatTower;
	}

	public void setFlatTower(String flatTower) {
		this.flatTower = flatTower;
	}

	public String getFlatBlock() {
		return flatBlock;
	}

	public void setFlatBlock(String flatBlock) {
		this.flatBlock = flatBlock;
	}

	public LocalDateTime getFlatPossnDate() {
		return flatPossnDate;
	}

	public void setFlatPossnDate(LocalDateTime flatPossnDate) {
		this.flatPossnDate = flatPossnDate;
	}

	public String getFlatOwnerType() {
		return flatOwnerType;
	}

	public void setFlatOwnerType(String flatOwnerType) {
		this.flatOwnerType = flatOwnerType;
	}

	public String getFlatArea() {
		return flatArea;
	}

	public void setFlatArea(String flatArea) {
		this.flatArea = flatArea;
	}

	public String getFlatPndngPaymntLst() {
		return flatPndngPaymntLst;
	}

	public void setFlatPndngPaymntLst(String flatPndngPaymntLst) {
		this.flatPndngPaymntLst = flatPndngPaymntLst;
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
