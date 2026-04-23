package com.secura.dnft.request.response;

import java.time.LocalDateTime;
import java.util.List;

public class TransactionResponseItem {

	private String aprmntId;
	private String trnscId;
	private LocalDateTime trnsDate;
	private String trnsBy;
	private List<PaymentTenderData> trnsTender;
	private String trnsType;
	private String trnsShrtDesc;
	private List<String> trnsFiles;
	private String trnsBnkAccnt;
	private String trnsAmt;
	private String trnsCurrency;
	private String pymntId;
	private String trnsStatus;
	private String noOfPerson;
	private String thirdPartyTrnsRef;
	private String thirdPartyName;
	private DueAmountDetails dueDetails;
	private String cause;
	private List<BankInstrumentTenderDetails> bankInstrumentTenderDetails;
	private String workListId;
	private String receiptNumber;
	private LocalDateTime creatTs;
	private String creatUsrId;
	private LocalDateTime lstUpdtTs;
	private String lstUpdtUsrId;

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getTrnscId() {
		return trnscId;
	}

	public void setTrnscId(String trnscId) {
		this.trnscId = trnscId;
	}

	public LocalDateTime getTrnsDate() {
		return trnsDate;
	}

	public void setTrnsDate(LocalDateTime trnsDate) {
		this.trnsDate = trnsDate;
	}

	public String getTrnsBy() {
		return trnsBy;
	}

	public void setTrnsBy(String trnsBy) {
		this.trnsBy = trnsBy;
	}

	public List<PaymentTenderData> getTrnsTender() {
		return trnsTender;
	}

	public void setTrnsTender(List<PaymentTenderData> trnsTender) {
		this.trnsTender = trnsTender;
	}

	public String getTrnsType() {
		return trnsType;
	}

	public void setTrnsType(String trnsType) {
		this.trnsType = trnsType;
	}

	public String getTrnsShrtDesc() {
		return trnsShrtDesc;
	}

	public void setTrnsShrtDesc(String trnsShrtDesc) {
		this.trnsShrtDesc = trnsShrtDesc;
	}

	public List<String> getTrnsFiles() {
		return trnsFiles;
	}

	public void setTrnsFiles(List<String> trnsFiles) {
		this.trnsFiles = trnsFiles;
	}

	public String getTrnsBnkAccnt() {
		return trnsBnkAccnt;
	}

	public void setTrnsBnkAccnt(String trnsBnkAccnt) {
		this.trnsBnkAccnt = trnsBnkAccnt;
	}

	public String getTrnsAmt() {
		return trnsAmt;
	}

	public void setTrnsAmt(String trnsAmt) {
		this.trnsAmt = trnsAmt;
	}

	public String getTrnsCurrency() {
		return trnsCurrency;
	}

	public void setTrnsCurrency(String trnsCurrency) {
		this.trnsCurrency = trnsCurrency;
	}

	public String getPymntId() {
		return pymntId;
	}

	public void setPymntId(String pymntId) {
		this.pymntId = pymntId;
	}

	public String getTrnsStatus() {
		return trnsStatus;
	}

	public void setTrnsStatus(String trnsStatus) {
		this.trnsStatus = trnsStatus;
	}

	public String getNoOfPerson() {
		return noOfPerson;
	}

	public void setNoOfPerson(String noOfPerson) {
		this.noOfPerson = noOfPerson;
	}

	public String getThirdPartyTrnsRef() {
		return thirdPartyTrnsRef;
	}

	public void setThirdPartyTrnsRef(String thirdPartyTrnsRef) {
		this.thirdPartyTrnsRef = thirdPartyTrnsRef;
	}

	public String getThirdPartyName() {
		return thirdPartyName;
	}

	public void setThirdPartyName(String thirdPartyName) {
		this.thirdPartyName = thirdPartyName;
	}

	public DueAmountDetails getDueDetails() {
		return dueDetails;
	}

	public void setDueDetails(DueAmountDetails dueDetails) {
		this.dueDetails = dueDetails;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public List<BankInstrumentTenderDetails> getBankInstrumentTenderDetails() {
		return bankInstrumentTenderDetails;
	}

	public void setBankInstrumentTenderDetails(List<BankInstrumentTenderDetails> bankInstrumentTenderDetails) {
		this.bankInstrumentTenderDetails = bankInstrumentTenderDetails;
	}

	public String getWorkListId() {
		return workListId;
	}

	public void setWorkListId(String workListId) {
		this.workListId = workListId;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String receiptNumber) {
		this.receiptNumber = receiptNumber;
	}

	public LocalDateTime getCreatTs() {
		return creatTs;
	}

	public void setCreatTs(LocalDateTime creatTs) {
		this.creatTs = creatTs;
	}

	public String getCreatUsrId() {
		return creatUsrId;
	}

	public void setCreatUsrId(String creatUsrId) {
		this.creatUsrId = creatUsrId;
	}

	public LocalDateTime getLstUpdtTs() {
		return lstUpdtTs;
	}

	public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
		this.lstUpdtTs = lstUpdtTs;
	}

	public String getLstUpdtUsrId() {
		return lstUpdtUsrId;
	}

	public void setLstUpdtUsrId(String lstUpdtUsrId) {
		this.lstUpdtUsrId = lstUpdtUsrId;
	}
}
