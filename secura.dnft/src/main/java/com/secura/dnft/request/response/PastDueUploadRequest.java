package com.secura.dnft.request.response;

import java.math.BigDecimal;

public class PastDueUploadRequest {

	private String flatId;
	private String dueFrom;
	private String dueTill;
	private String dueName;
	private BigDecimal dueAmount;
	private BigDecimal penaltyAmount;
	private String fineEligible;
	private BigDecimal finePercentage;
	private String fineType;
	private String reason;

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public String getDueFrom() {
		return dueFrom;
	}

	public void setDueFrom(String dueFrom) {
		this.dueFrom = dueFrom;
	}

	public String getDueTill() {
		return dueTill;
	}

	public void setDueTill(String dueTill) {
		this.dueTill = dueTill;
	}

	public String getDueName() {
		return dueName;
	}

	public void setDueName(String dueName) {
		this.dueName = dueName;
	}

	public BigDecimal getDueAmount() {
		return dueAmount;
	}

	public void setDueAmount(BigDecimal dueAmount) {
		this.dueAmount = dueAmount;
	}

	public BigDecimal getPenaltyAmount() {
		return penaltyAmount;
	}

	public void setPenaltyAmount(BigDecimal penaltyAmount) {
		this.penaltyAmount = penaltyAmount;
	}

	public String getFineEligible() {
		return fineEligible;
	}

	public void setFineEligible(String fineEligible) {
		this.fineEligible = fineEligible;
	}

	public BigDecimal getFinePercentage() {
		return finePercentage;
	}

	public void setFinePercentage(BigDecimal finePercentage) {
		this.finePercentage = finePercentage;
	}

	public String getFineType() {
		return fineType;
	}

	public void setFineType(String fineType) {
		this.fineType = fineType;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
