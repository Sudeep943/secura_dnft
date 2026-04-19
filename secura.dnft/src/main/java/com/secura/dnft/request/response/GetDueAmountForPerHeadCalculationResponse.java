package com.secura.dnft.request.response;

public class GetDueAmountForPerHeadCalculationResponse {

	private GenericHeader genericHeader;
	private DueAmountDetails dueAmountDetails;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public DueAmountDetails getDueAmountDetails() {
		return dueAmountDetails;
	}

	public void setDueAmountDetails(DueAmountDetails dueAmountDetails) {
		this.dueAmountDetails = dueAmountDetails;
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
