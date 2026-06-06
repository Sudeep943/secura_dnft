package com.secura.dnft.request.response;

import java.util.List;

public class TagDiscFinFromPaymentResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private String discFinId;
	private List<String> discFinCodes;

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

	public String getDiscFinId() {
		return discFinId;
	}

	public void setDiscFinId(String discFinId) {
		this.discFinId = discFinId;
	}

	public List<String> getDiscFinCodes() {
		return discFinCodes;
	}

	public void setDiscFinCodes(List<String> discFinCodes) {
		this.discFinCodes = discFinCodes;
	}
}
