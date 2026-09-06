package com.secura.dnft.request.response;

import java.math.BigDecimal;
import java.util.List;

import com.secura.dnft.entity.PaymentEntity;

public class GetPaymentDetailsResponse {

	private GenericHeader genericHeader;
	private List<CompletedPaymentDetails> completedPaymentDetails;
	private BigDecimal totalCollection;
	private BigDecimal totalNoOfPerson;
	private String message;
	private String messageCode;
	private PaymentEntity paymentDetail;
	

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<CompletedPaymentDetails> getCompletedPaymentDetails() {
		return completedPaymentDetails;
	}

	public void setCompletedPaymentDetails(List<CompletedPaymentDetails> completedPaymentDetails) {
		this.completedPaymentDetails = completedPaymentDetails;
	}

	public BigDecimal getTotalCollection() {
		return totalCollection;
	}

	public void setTotalCollection(BigDecimal totalCollection) {
		this.totalCollection = totalCollection;
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

	public PaymentEntity getPaymentDetail() {
		return paymentDetail;
	}

	public void setPaymentDetail(PaymentEntity paymentDetail) {
		this.paymentDetail = paymentDetail;
	}

	public BigDecimal getTotalNoOfPerson() {
		return totalNoOfPerson;
	}

	public void setTotalNoOfPerson(BigDecimal totalNoOfPerson) {
		this.totalNoOfPerson = totalNoOfPerson;
	}
	
}
