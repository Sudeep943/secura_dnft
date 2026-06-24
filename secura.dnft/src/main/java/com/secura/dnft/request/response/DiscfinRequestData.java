package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

public class DiscfinRequestData {

	private String discFnType;
	private Boolean dueDateAsStartDateFlag;
	private Date discFnStrtDt;
	private Date discFnEndDt;
	private String discFnMode;
	private String discFnCumlatonCycle;
	private String discFnCycleType;
	private String discFnValue;
	private List<DiscFinCycleDiscount> discFinCycleDiscountList;
	private String minimumPaymentAmount;
	private Boolean partOfCycleAsFull;
	private String bufferTime;
	private String bufferTimeUnit;

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

	public Date getDiscFnStrtDt() {
		return discFnStrtDt;
	}

	public void setDiscFnStrtDt(Date discFnStrtDt) {
		this.discFnStrtDt = discFnStrtDt;
	}

	public Date getDiscFnEndDt() {
		return discFnEndDt;
	}

	public void setDiscFnEndDt(Date discFnEndDt) {
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

	public String getDiscFnValue() {
		return discFnValue;
	}

	public void setDiscFnValue(String discFnValue) {
		this.discFnValue = discFnValue;
	}

	public List<DiscFinCycleDiscount> getDiscFinCycleDiscountList() {
		return discFinCycleDiscountList;
	}

	public void setDiscFinCycleDiscountList(List<DiscFinCycleDiscount> discFinCycleDiscountList) {
		this.discFinCycleDiscountList = discFinCycleDiscountList;
	}

	public String getMinimumPaymentAmount() {
		return minimumPaymentAmount;
	}

	public void setMinimumPaymentAmount(String minimumPaymentAmount) {
		this.minimumPaymentAmount = minimumPaymentAmount;
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
}
