package com.secura.dnft.request.response;

import java.util.List;
import java.util.Map;

public class GetDueAmountForFlatResponse {

	private GenericHeader genericHeader;
	private Map<String, List<DueAmountDetails>> duePaymentList;
	private String totalDueAmount;
	private String totalMandatoryPaymentAmount;
	private String totalOptionalPaymentAmount;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public Map<String, List<DueAmountDetails>> getDuePaymentList() {
		return duePaymentList;
	}

	public void setDuePaymentList(Map<String, List<DueAmountDetails>> duePaymentList) {
		this.duePaymentList = duePaymentList;
	}

	public String getTotalDueAmount() {
		return totalDueAmount;
	}

	public void setTotalDueAmount(String totalDueAmount) {
		this.totalDueAmount = totalDueAmount;
	}

	public String getTotalMandatoryPaymentAmount() {
		return totalMandatoryPaymentAmount;
	}

	public void setTotalMandatoryPaymentAmount(String totalMandatoryPaymentAmount) {
		this.totalMandatoryPaymentAmount = totalMandatoryPaymentAmount;
	}

	public String getTotalOptionalPaymentAmount() {
		return totalOptionalPaymentAmount;
	}

	public void setTotalOptionalPaymentAmount(String totalOptionalPaymentAmount) {
		this.totalOptionalPaymentAmount = totalOptionalPaymentAmount;
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
