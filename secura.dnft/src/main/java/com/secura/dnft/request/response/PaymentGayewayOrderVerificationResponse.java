package com.secura.dnft.request.response;

import java.util.Map;

public class PaymentGayewayOrderVerificationResponse {

	private String message;
	private String messageCode;
	private Map<String, Object> data;
	
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
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	
	
}
