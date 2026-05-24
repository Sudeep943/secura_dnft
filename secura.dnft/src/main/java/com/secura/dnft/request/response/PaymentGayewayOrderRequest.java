package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.Map;

public class PaymentGayewayOrderRequest {

	private GenericHeader genericHeader;
	private String paymentGateway;
	private String amountInPaisa;
	private String currency;
	private Date eventDate;
	private String transactionType;
	private Map<String, Object> data;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getPaymentGateway() {
		return paymentGateway;
	}
	public void setPaymentGateway(String paymentGateway) {
		this.paymentGateway = paymentGateway;
	}
	public String getAmountInPaisa() {
		return amountInPaisa;
	}
	public void setAmountInPaisa(String amountInPaisa) {
		this.amountInPaisa = amountInPaisa;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Date getEventDate() {
		return eventDate;
	}
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
	public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	
}
