package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

public class GetBalanceSheetResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private Date fromDate;
	private Date toDate;
	private List<TransactionResponseItem> creditPaymentData;
	private List<TransactionResponseItem> debitPaymentData;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}

	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	public List<TransactionResponseItem> getCreditPaymentData() {
		return creditPaymentData;
	}

	public void setCreditPaymentData(List<TransactionResponseItem> creditPaymentData) {
		this.creditPaymentData = creditPaymentData;
	}

	public List<TransactionResponseItem> getDebitPaymentData() {
		return debitPaymentData;
	}

	public void setDebitPaymentData(List<TransactionResponseItem> debitPaymentData) {
		this.debitPaymentData = debitPaymentData;
	}
}
