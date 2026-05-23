package com.secura.dnft.request.response;

public class AddBankDetailsResponse {

	private GenericHeader genericHeader;
	private String bankDetailsID;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getBankDetailsID() {
		return bankDetailsID;
	}

	public void setBankDetailsID(String bankDetailsID) {
		this.bankDetailsID = bankDetailsID;
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
