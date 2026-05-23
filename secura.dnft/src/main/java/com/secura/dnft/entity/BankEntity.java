package com.secura.dnft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(BankEntityId.class)
@Table(name = "bankentity")
public class BankEntity {

	@Id
	@Column(name = "aprmnt_id")
	private String aprmntId;

	@Id
	@Column(name = "BankDetailsID")
	private String bankDetailsID;

	@Column(name = "bankName", columnDefinition = "TEXT")
	private String bankName;

	@Column(name = "accountNumber", columnDefinition = "TEXT")
	private String accountNumber;

	@Column(name = "ifscCode", columnDefinition = "TEXT")
	private String ifscCode;

	@Column(name = "branch", columnDefinition = "TEXT")
	private String branch;

	@Column(name = "accountName", columnDefinition = "TEXT")
	private String accountName;

	@Column(name = "pgKey", columnDefinition = "TEXT")
	private String pgKey;

	@Column(name = "PgSecret", columnDefinition = "TEXT")
	private String pgSecret;

	@Column(name = "pgName", columnDefinition = "TEXT")
	private String pgName;

	@Column(name = "upiId", columnDefinition = "TEXT")
	private String upiId;

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

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
