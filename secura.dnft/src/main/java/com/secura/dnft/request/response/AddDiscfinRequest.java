package com.secura.dnft.request.response;

import java.sql.Date;

public class AddDiscfinRequest {

	private GenericHeader genericHeader;
	private String discFnType;
	private Boolean dueDateAsStartDateFlag;
	private Date discFnStrtDt;
	private Date discFnEndDt;
	private String discFnMode;
	private String discFnCumlatonCycle;
	private String discFnCycleType;

	public GenericHeader getGenericHeader() {
		return genericHeader;
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
}
