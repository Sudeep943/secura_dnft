package com.secura.dnft.request.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.secura.dnft.entity.DueAmountDetailsEntity;

public class GetDueAmountForFlatResponse {

	private GenericHeader genericHeader;
	@JsonSerialize(keyUsing = PaymentDetailKeySerializer.class)
	private Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails;
	private String totalDue;
	private Boolean penaltyAdded;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public Map<PaymentDetail, List<DueAmountDetailsEntity>> getDueDetails() {
		return dueDetails;
	}

	public void setDueDetails(Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails) {
		this.dueDetails = dueDetails;
	}

	public String getTotalDue() {
		return totalDue;
	}

	public void setTotalDue(String totalDue) {
		this.totalDue = totalDue;
	}

	public Boolean getPenaltyAdded() {
		return penaltyAdded;
	}

	public void setPenaltyAdded(Boolean penaltyAdded) {
		this.penaltyAdded = penaltyAdded;
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
