package com.secura.dnft.request.response;

import com.secura.dnft.entity.PaymentEntity;

public class GetPaymentUtilDetailsResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private PaymentEntity paymentEntity;
	private String expectedCollection;
	private String totalCollection;
	private String totalPendingTransactionAmount;
	private String collectionPercentage;

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

	public PaymentEntity getPaymentEntity() {
		return paymentEntity;
	}

	public void setPaymentEntity(PaymentEntity paymentEntity) {
		this.paymentEntity = paymentEntity;
	}

	public String getExpectedCollection() {
		return expectedCollection;
	}

	public void setExpectedCollection(String expectedCollection) {
		this.expectedCollection = expectedCollection;
	}

	public String getTotalCollection() {
		return totalCollection;
	}

	public void setTotalCollection(String totalCollection) {
		this.totalCollection = totalCollection;
	}

	public String getTotalPendingTransactionAmount() {
		return totalPendingTransactionAmount;
	}

	public void setTotalPendingTransactionAmount(String totalPendingTransactionAmount) {
		this.totalPendingTransactionAmount = totalPendingTransactionAmount;
	}

	public String getCollectionPercentage() {
		return collectionPercentage;
	}

	public void setCollectionPercentage(String collectionPercentage) {
		this.collectionPercentage = collectionPercentage;
	}
}
