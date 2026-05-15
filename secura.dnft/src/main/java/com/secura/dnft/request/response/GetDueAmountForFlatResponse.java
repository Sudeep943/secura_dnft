package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.DueAmountDetailsEntity;

public class GetDueAmountForFlatResponse {

	private GenericHeader genericHeader;
	private List<DueDetailGroup> dueDetails;
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

	public List<DueDetailGroup> getDueDetails() {
		return dueDetails;
	}

	public void setDueDetails(List<DueDetailGroup> dueDetails) {
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

	public static class DueDetailGroup {
		private PaymentDetail paymentDetail;
		private List<DueAmountDetailsEntity> dues;

		public PaymentDetail getPaymentDetail() {
			return paymentDetail;
		}

		public void setPaymentDetail(PaymentDetail paymentDetail) {
			this.paymentDetail = paymentDetail;
		}

		public List<DueAmountDetailsEntity> getDues() {
			return dues;
		}

		public void setDues(List<DueAmountDetailsEntity> dues) {
			this.dues = dues;
		}
	}
}
