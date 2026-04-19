package com.secura.dnft.bean;

public class BankAccountDetails {

	private String bankName;
	private String accountNumber;
	private String ifscCode;
	private String branch;
	private String accountName;
	private String razorPayKey;
	private String razorPaySecret;

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public void setIfscCode(String ifscCode) {
		this.ifscCode = ifscCode;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getRazorPayKey() {
		return razorPayKey;
	}

	public void setRazorPayKey(String razorPayKey) {
		this.razorPayKey = razorPayKey;
	}

	public String getRazorPaySecret() {
		return razorPaySecret;
	}

	public void setRazorPaySecret(String razorPaySecret) {
		this.razorPaySecret = razorPaySecret;
	}
}
