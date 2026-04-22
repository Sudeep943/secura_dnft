package com.secura.dnft.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_trnsac")
public class Transaction {

	@Column(name = "aprmnt_id")
	private String aprmntId;

	@Id
	@Column(name = "trnsc_id")
	private String trnscId;

	@Column(name = "trns_date")
	private LocalDateTime trnsDate;

	@Column(name = "trns_by")
	private String trnsBy;

	@Column(name = "trns_tender", columnDefinition = "TEXT")
	private String trnsTender;

	@Column(name = "trns_type")
	private String trnsType;

	@Column(name = "trns_shrt_desc")
	private String trnsShrtDesc;

	@Column(name = "trns_files", columnDefinition = "TEXT")
	private String trnsFiles;

	@Column(name = "trns_bnk_accnt")
	private String trnsBnkAccnt;

	@Column(name = "trns_amt")
	private String trnsAmt;

	@Column(name = "trns_currency")
	private String trnsCurrency;

	@Column(name = "pymnt_id")
	private String pymntId;

	@Column(name = "trns_status")
	private String trnsStatus;

	@Column(name = "no_of_person")
	private String noOfPerson;

	@Column(name = "third_party_trns_ref")
	private String thirdPartyTrnsRef;

	@Column(name = "third_party_name")
	private String thirdPartyName;

	@Column(name = "due_details", columnDefinition = "TEXT")
	private String dueDetails;

	@Column(name = "cause")
	private String cause;

	@Column(name = "bank_instrument_tender_details", columnDefinition = "TEXT")
	private String bankInstrumentTenderDetails;

	@Column(name = "worklist_id")
	private String workListId;

	@Column(name = "receipt_number")
	private String receiptNumber;

	@Column(name = "creat_ts")
	private LocalDateTime creatTs;

	@Column(name = "creat_usr_id")
	private String creatUsrId;

	@Column(name = "lst_updt_ts")
	private LocalDateTime lstUpdtTs;

	@Column(name = "lst_updt_usr_id")
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

	public String getTrnsTender() {
		return trnsTender;
	}

	public void setTrnsTender(String trnsTender) {
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

	public String getTrnsFiles() {
		return trnsFiles;
	}

	public void setTrnsFiles(String trnsFiles) {
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

	public String getDueDetails() {
		return dueDetails;
	}

	public void setDueDetails(String dueDetails) {
		this.dueDetails = dueDetails;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public String getBankInstrumentTenderDetails() {
		return bankInstrumentTenderDetails;
	}

	public void setBankInstrumentTenderDetails(String bankInstrumentTenderDetails) {
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
