package com.secura.dnft.request.response;

import java.util.List;

public class CreateOtpResponse {

	private String message;
	private String messageCode;
	private String otpId;
	private List<String> mailIds;

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

	public String getOtpId() {
		return otpId;
	}

	public void setOtpId(String otpId) {
		this.otpId = otpId;
	}

	public List<String> getMailIds() {
		return mailIds;
	}

	public void setMailIds(List<String> mailIds) {
		this.mailIds = mailIds;
	}
}
