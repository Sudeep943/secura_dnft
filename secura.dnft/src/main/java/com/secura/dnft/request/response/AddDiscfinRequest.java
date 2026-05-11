package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class AddDiscfinRequest {

	private GenericHeader genericHeader;
	private String discFnType;
	private Boolean dueDateAsStartDateFlag;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private Date discFnStrtDt;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private Date discFnEndDt;
	private String discFnMode;
	private String discFnCumlatonCycle;
	private String discFnCycleType;
	private String discFnValue;
	private List<DiscFinCycleDiscount> discFinCycleDiscountList;
	private String minimumPaymentAmount;


	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public String getDiscFnValue() {
		return discFnValue;
	}

	public void setDiscFnValue(String discFnValue) {
		this.discFnValue = discFnValue;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
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

}
