package com.secura.dnft.request.response;

public class GetLetterHeadResponse {
	private GenericHeader genericHeader;
	private String letterHead;
	private String message;
    private String messageCode;
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getLetterHead() {
		return letterHead;
	}
	public void setLetterHead(String letterHead) {
		this.letterHead = letterHead;
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
