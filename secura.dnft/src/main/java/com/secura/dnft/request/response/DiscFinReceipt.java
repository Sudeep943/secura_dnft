package com.secura.dnft.request.response;

public class DiscFinReceipt {

	private String discountPercentage;
	private String discountType;
	private String discountAmount;
	private String finePercentage;
	private String fineType;
	private String fineAmount;
	private String fineCycleMode;

	public String getDiscountPercentage() {
		return discountPercentage;
	}

	public void setDiscountPercentage(String discountPercentage) {
		this.discountPercentage = discountPercentage;
	}

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}

	public String getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(String discountAmount) {
		this.discountAmount = discountAmount;
	}

	public String getFinePercentage() {
		return finePercentage;
	}

	public void setFinePercentage(String finePercentage) {
		this.finePercentage = finePercentage;
	}

	public String getFineType() {
		return fineType;
	}

	public void setFineType(String fineType) {
		this.fineType = fineType;
	}

	public String getFineAmount() {
		return fineAmount;
	}

	public void setFineAmount(String fineAmount) {
		this.fineAmount = fineAmount;
	}

	public String getFineCycleMode() {
		return fineCycleMode;
	}

	public void setFineCycleMode(String fineCycleMode) {
		this.fineCycleMode = fineCycleMode;
	}
}
