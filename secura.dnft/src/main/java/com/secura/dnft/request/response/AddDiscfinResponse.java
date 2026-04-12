package com.secura.dnft.request.response;

public class AddDiscfinResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private String discFnId;

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

	public String getDiscFnId() {
		return discFnId;
	}

	public void setDiscFnId(String discFnId) {
		this.discFnId = discFnId;
	}
}
