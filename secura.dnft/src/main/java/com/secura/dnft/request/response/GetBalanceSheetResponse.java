package com.secura.dnft.request.response;

import java.time.LocalDate;
import java.util.List;

public class GetBalanceSheetResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private LocalDate fromDate;
	private LocalDate toDate;
	private List<ReportPaymentData> creditPaymentData;
	private List<ReportPaymentData> debitPaymentData;

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

	public LocalDate getFromDate() {
		return fromDate;
	}

	public void setFromDate(LocalDate fromDate) {
		this.fromDate = fromDate;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public void setToDate(LocalDate toDate) {
		this.toDate = toDate;
	}

	public List<ReportPaymentData> getCreditPaymentData() {
		return creditPaymentData;
	}

	public void setCreditPaymentData(List<ReportPaymentData> creditPaymentData) {
		this.creditPaymentData = creditPaymentData;
	}

	public List<ReportPaymentData> getDebitPaymentData() {
		return debitPaymentData;
	}

	public void setDebitPaymentData(List<ReportPaymentData> debitPaymentData) {
		this.debitPaymentData = debitPaymentData;
	}
}
