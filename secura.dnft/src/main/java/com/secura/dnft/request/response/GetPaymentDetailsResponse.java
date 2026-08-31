package com.secura.dnft.request.response;

import java.math.BigDecimal;
import java.util.List;

public class GetPaymentDetailsResponse {

	private GenericHeader genericHeader;
	private List<CompletedPaymentDetails> completedPaymentDetails;
	private BigDecimal totalCollection;
	private String message;
	private String messageCode;

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
}
