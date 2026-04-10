package com.secura.dnft.request.response;

public class UpdateFlatDetailsResponse {

	private GenericHeader header;
	private String flatNo;
	private String message;
	private String messageCode;

	public GenericHeader getHeader() {
		return header;
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
	}

	public String getFlatNo() {
		return flatNo;
	}

	public void setFlatNo(String flatNo) {
		this.flatNo = flatNo;
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
