package com.secura.dnft.request.response;

import java.util.List;

public class Defaulter {

	private String flatId;
	private String builtUpArea;
	private List<String> ownerNames;
	private String phoneNumber;
	private String emailId;
	private List<DefaultPayment> defaultPaymentList;

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public String getBuiltUpArea() {
		return builtUpArea;
	}

	public void setBuiltUpArea(String builtUpArea) {
		this.builtUpArea = builtUpArea;
	}

	public List<String> getOwnerNames() {
		return ownerNames;
	}

	public void setOwnerNames(List<String> ownerNames) {
		this.ownerNames = ownerNames;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public List<DefaultPayment> getDefaultPaymentList() {
		return defaultPaymentList;
	}

	public void setDefaultPaymentList(List<DefaultPayment> defaultPaymentList) {
		this.defaultPaymentList = defaultPaymentList;
	}
}
