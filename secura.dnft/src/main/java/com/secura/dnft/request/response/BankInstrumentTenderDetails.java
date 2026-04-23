package com.secura.dnft.request.response;

import java.sql.Date;

public class BankInstrumentTenderDetails {

	private String tenderType;
	private String chequeNumber;
	private Date chequeDate;
	private String bankName;
	private String accountHolderName;
	private String amount;
	private String ddPayAtBranch;
	private String ddNumber;
	private Date ddIssueDate;
	private String remarks;

	public String getTenderType() {
		return tenderType;
	}

	public void setTenderType(String tenderType) {
		this.tenderType = tenderType;
	}

	public String getChequeNumber() {
		return chequeNumber;
	}

	public void setChequeNumber(String chequeNumber) {
		this.chequeNumber = chequeNumber;
	}

	public Date getChequeDate() {
		return chequeDate;
	}

	public void setChequeDate(Date chequeDate) {
		this.chequeDate = chequeDate;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getAccountHolderName() {
		return accountHolderName;
	}

	public void setAccountHolderName(String accountHolderName) {
		this.accountHolderName = accountHolderName;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getDdPayAtBranch() {
		return ddPayAtBranch;
	}

	public void setDdPayAtBranch(String ddPayAtBranch) {
		this.ddPayAtBranch = ddPayAtBranch;
	}

	public String getDdNumber() {
		return ddNumber;
	}

	public void setDdNumber(String ddNumber) {
		this.ddNumber = ddNumber;
	}

	public Date getDdIssueDate() {
		return ddIssueDate;
	}

	public void setDdIssueDate(Date ddIssueDate) {
		this.ddIssueDate = ddIssueDate;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
}
