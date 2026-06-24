package com.secura.dnft.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(DiscFinId.class)
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
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate discFnStrtDt;

	@Column(name = "disc_fn_end_dt")
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate discFnEndDt;

	@Column(name = "disc_fn_mode")
	private String discFnMode;

	@Column(name = "disc_fn_cumlaton_cycle")
	private String discFnCumlatonCycle;

	@Id
	@Column(name = "disc_fn_cycle_type")
	private String discFnCycleType;

	@Column(name = "minimum_payment_amount")
	private String minimumPaymentAmount;

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
	
	@Column(name = "fn_calculation_type")
	private String fnCalculationType;

	@Column(name = "disc_fn_value")
	private String discFinValue;

	private Boolean partOfCycleAsFull;

	@Column(length = 255)
	private String bufferTime;

	@Column(length = 255)
	private String bufferTimeUnit;

	public String getFnCalculationType() {
		return fnCalculationType;
	}

	public void setFnCalculationType(String fnCalculationType) {
		this.fnCalculationType = fnCalculationType;
	}

	public String getDiscFinValue() {
		return discFinValue;
	}

	public void setDiscFinValue(String discFinValue) {
		this.discFinValue = discFinValue;
	}

	public Boolean getPartOfCycleAsFull() {
		return partOfCycleAsFull;
	}

	public void setPartOfCycleAsFull(Boolean partOfCycleAsFull) {
		this.partOfCycleAsFull = partOfCycleAsFull;
	}

	public String getBufferTime() {
		return bufferTime;
	}

	public void setBufferTime(String bufferTime) {
		this.bufferTime = bufferTime;
	}

	public String getBufferTimeUnit() {
		return bufferTimeUnit;
	}

	public void setBufferTimeUnit(String bufferTimeUnit) {
		this.bufferTimeUnit = bufferTimeUnit;
	}

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

	public LocalDate getDiscFnStrtDt() {
		return discFnStrtDt;
	}

	public void setDiscFnStrtDt(LocalDate discFnStrtDt) {
		this.discFnStrtDt = discFnStrtDt;
	}

	public LocalDate getDiscFnEndDt() {
		return discFnEndDt;
	}

	public void setDiscFnEndDt(LocalDate discFnEndDt) {
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

	public String getDiscFnCycleType() {
		return discFnCycleType;
	}

	public void setDiscFnCycleType(String discFnCycleType) {
		this.discFnCycleType = discFnCycleType;
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

	public String getMinimumPaymentAmount() {
		return minimumPaymentAmount;
	}

	public void setMinimumPaymentAmount(String minimumPaymentAmount) {
		this.minimumPaymentAmount = minimumPaymentAmount;
	}
}
