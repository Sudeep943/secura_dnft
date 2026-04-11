package com.secura.dnft.request.response;

import java.sql.Date;

public class CreatePaymentRequest {

	private GenericHeader genericHeader;
	
	private String paymentName;
	private String shortDetails;
	private String paymentCapita;
	private String paymentAmount;
	private String gst;
	private String currency;
	private Date collectionStartDate;
	private Date collectionEndDate;
	private String paymentCollectionCycle;
	private String paymentCollectionMode;
	private String applicableFor;
	private String paymentType;
	private String bankAccountId;
	private String status;
	private boolean camPayment;
	private boolean addLeftOverPayment;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getPaymentName() {
		return paymentName;
	}
	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}
	public String getShortDetails() {
		return shortDetails;
	}
	public void setShortDetails(String shortDetails) {
		this.shortDetails = shortDetails;
	}
	public String getPaymentCapita() {
		return paymentCapita;
	}
	public void setPaymentCapita(String paymentCapita) {
		this.paymentCapita = paymentCapita;
	}
	public String getPaymentAmount() {
		return paymentAmount;
	}
	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}
	public String getGst() {
		return gst;
	}
	public void setGst(String gst) {
		this.gst = gst;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Date getCollectionStartDate() {
		return collectionStartDate;
	}
	public void setCollectionStartDate(Date collectionStartDate) {
		this.collectionStartDate = collectionStartDate;
	}
	public Date getCollectionEndDate() {
		return collectionEndDate;
	}
	public void setCollectionEndDate(Date collectionEndDate) {
		this.collectionEndDate = collectionEndDate;
	}
	public String getPaymentCollectionCycle() {
		return paymentCollectionCycle;
	}
	public void setPaymentCollectionCycle(String paymentCollectionCycle) {
		this.paymentCollectionCycle = paymentCollectionCycle;
	}
	public String getPaymentCollectionMode() {
		return paymentCollectionMode;
	}
	public void setPaymentCollectionMode(String paymentCollectionMode) {
		this.paymentCollectionMode = paymentCollectionMode;
	}
	public String getApplicableFor() {
		return applicableFor;
	}
	public void setApplicableFor(String applicableFor) {
		this.applicableFor = applicableFor;
	}
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	public String getBankAccountId() {
		return bankAccountId;
	}
	public void setBankAccountId(String bankAccountId) {
		this.bankAccountId = bankAccountId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public boolean isCamPayment() {
		return camPayment;
	}
	public void setCamPayment(boolean camPayment) {
		this.camPayment = camPayment;
	}
	public boolean isAddLeftOverPayment() {
		return addLeftOverPayment;
	}
	public void setAddLeftOverPayment(boolean addLeftOverPayment) {
		this.addLeftOverPayment = addLeftOverPayment;
	}
	
	
	

}
