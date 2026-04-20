package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

public class LedgerEntryRequest {

	private GenericHeader genericHeader;
	private Date trnsDate;
	private String ledgerfor;
	private List<String> trnsTenderList;
	private String trnsType;
	private String trnsShrtDesc;
	private String trnsBnkAccnt;
	private String trnsAmt;
	private String trnsStatus;
	private String cause;
	private List<String> supportedFile;
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

	public List<String> getTrnsTenderList() {
		return trnsTenderList;
	}

	public void setTrnsTenderList(List<String> trnsTenderList) {
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

	public List<String> getSupportedFile() {
		return supportedFile;
	}

	public void setSupportedFile(List<String> supportedFile) {
		this.supportedFile = supportedFile;
	}

	public boolean isRequiredReceiptFlag() {
		return requiredReceiptFlag;
	}

	public void setRequiredReceiptFlag(boolean requiredReceiptFlag) {
		this.requiredReceiptFlag = requiredReceiptFlag;
	}
}
