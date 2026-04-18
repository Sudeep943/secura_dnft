package com.secura.dnft.request.response;

public class Receipt {

	private String receiptNumber;
	private String receiptType;
	private String receiptDate;
	private String receiptFileName;
	private String receiptFileType;
	private String receiptFileData;

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String receiptNumber) {
		this.receiptNumber = receiptNumber;
	}

	public String getReceiptType() {
		return receiptType;
	}

	public void setReceiptType(String receiptType) {
		this.receiptType = receiptType;
	}

	public String getReceiptDate() {
		return receiptDate;
	}

	public void setReceiptDate(String receiptDate) {
		this.receiptDate = receiptDate;
	}

	public String getReceiptFileName() {
		return receiptFileName;
	}

	public void setReceiptFileName(String receiptFileName) {
		this.receiptFileName = receiptFileName;
	}

	public String getReceiptFileType() {
		return receiptFileType;
	}

	public void setReceiptFileType(String receiptFileType) {
		this.receiptFileType = receiptFileType;
	}

	public String getReceiptFileData() {
		return receiptFileData;
	}

	public void setReceiptFileData(String receiptFileData) {
		this.receiptFileData = receiptFileData;
	}
}
