package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.secura.dnft.entity.DocumentEntity;

public class LedgerEntryRequest {

	private GenericHeader genericHeader;
	private Date trnsDate;
	private String ledgerfor;
	private List<PaymentTenderData> trnsTenderList;
	private String trnsType;
	private String trnsShrtDesc;
	private String trnsBnkAccnt;
	private String trnsAmt;
	private String trnsStatus;
	private String cause;
	@JsonProperty("supportedFileList")
	@JsonAlias("supportedFile")
	private List<DocumentEntity> supportedFileList;
	private boolean requiredReceiptFlag;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public Date getTrnsDate() {
		return trnsDate;
	}

	public void setTrnsDate(Date trnsDate) {
		this.trnsDate = trnsDate;
	}

	public String getLedgerfor() {
		return ledgerfor;
	}

	public void setLedgerfor(String ledgerfor) {
		this.ledgerfor = ledgerfor;
	}

	public List<PaymentTenderData> getTrnsTenderList() {
		return trnsTenderList;
	}

	public void setTrnsTenderList(List<PaymentTenderData> trnsTenderList) {
		this.trnsTenderList = trnsTenderList;
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

	public String getTrnsStatus() {
		return trnsStatus;
	}

	public void setTrnsStatus(String trnsStatus) {
		this.trnsStatus = trnsStatus;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public List<DocumentEntity> getSupportedFileList() {
		return supportedFileList;
	}

	public void setSupportedFileList(List<DocumentEntity> supportedFileList) {
		this.supportedFileList = supportedFileList;
	}

	public boolean isRequiredReceiptFlag() {
		return requiredReceiptFlag;
	}

	public void setRequiredReceiptFlag(boolean requiredReceiptFlag) {
		this.requiredReceiptFlag = requiredReceiptFlag;
	}
}
