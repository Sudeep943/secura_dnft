package com.secura.dnft.bean;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BankAccountDetails {

	@JsonProperty("BankDetailsID")
	@JsonAlias("bankDetailsID")
	private String bankDetailsID;
	private String bankName;
	private String accountNumber;
	private String ifscCode;
	private String branch;
	private String accountName;
	@JsonAlias("razorPayKey")
	private String pgKey;
	@JsonProperty("PgSecret")
	@JsonAlias({ "pgSecret", "razorPaySecret" })
	private String pgSecret;
	@JsonAlias({ "PgName", "razorPayName" })
	private String pgName;
	private String upiId;

	public String getBankDetailsID() {
		return bankDetailsID;
	}

	public void setBankDetailsID(String bankDetailsID) {
		this.bankDetailsID = bankDetailsID;
	}

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

	public String getPgKey() {
		return pgKey;
	}

	public void setPgKey(String pgKey) {
		this.pgKey = pgKey;
	}

	public String getPgSecret() {
		return pgSecret;
	}

	public void setPgSecret(String pgSecret) {
		this.pgSecret = pgSecret;
	}

	public String getUpiId() {
		return upiId;
	}

	public void setUpiId(String upiId) {
		this.upiId = upiId;
	}

	public String getPgName() {
		return pgName;
	}

	public void setPgName(String pgName) {
		this.pgName = pgName;
	}
}
