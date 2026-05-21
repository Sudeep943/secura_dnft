package com.secura.dnft.request.response;

public class PastDueUploadResponse {

	private String message;
	private String messageCode;
	private String errorfile;

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

	public String getErrorfile() {
		return errorfile;
	}

	public void setErrorfile(String errorfile) {
		this.errorfile = errorfile;
	}
}
