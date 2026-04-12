package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_disc_fin")
public class DiscFin {

	@Column(name = "aprmt_id")
	private String aprmtId;

	@Id
	@Column(name = "disc_fn_id", nullable = false)
	private String discFnId;

	@Column(name = "disc_fn_type")
	private String discFnType;

	@Column(name = "due_date_as_start_date_flag")
	private Boolean dueDateAsStartDateFlag;

	@Column(name = "disc_fn_strt_dt")
	private LocalDateTime discFnStrtDt;

	@Column(name = "disc_fn_end_dt")
	private LocalDateTime discFnEndDt;

	@Column(name = "disc_fn_mode")
	private String discFnMode;

	@Column(name = "disc_fn_cumlaton_cycle")
	private String discFnCumlatonCycle;

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

	public String getAprmtId() {
		return aprmtId;
	}

	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}

	public String getDiscFnId() {
		return discFnId;
	}

	public void setDiscFnId(String discFnId) {
		this.discFnId = discFnId;
	}

	public String getDiscFnType() {
		return discFnType;
	}

	public void setDiscFnType(String discFnType) {
		this.discFnType = discFnType;
	}

	public Boolean getDueDateAsStartDateFlag() {
		return dueDateAsStartDateFlag;
	}

	public void setDueDateAsStartDateFlag(Boolean dueDateAsStartDateFlag) {
		this.dueDateAsStartDateFlag = dueDateAsStartDateFlag;
	}

	public LocalDateTime getDiscFnStrtDt() {
		return discFnStrtDt;
	}

	public void setDiscFnStrtDt(LocalDateTime discFnStrtDt) {
		this.discFnStrtDt = discFnStrtDt;
	}

	public LocalDateTime getDiscFnEndDt() {
		return discFnEndDt;
	}

	public void setDiscFnEndDt(LocalDateTime discFnEndDt) {
		this.discFnEndDt = discFnEndDt;
	}

	public String getDiscFnMode() {
		return discFnMode;
	}

	public void setDiscFnMode(String discFnMode) {
		this.discFnMode = discFnMode;
	}

	public String getDiscFnCumlatonCycle() {
		return discFnCumlatonCycle;
	}

	public void setDiscFnCumlatonCycle(String discFnCumlatonCycle) {
		this.discFnCumlatonCycle = discFnCumlatonCycle;
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
