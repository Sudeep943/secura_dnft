package com.secura.dnft.request.response;

import java.util.List;
import java.util.Map;

public class LoginResponse {

    private Map<String,List<String>> accountDetails;
	
	 public Map<String,List<String>> getAccountDetails() {
		return accountDetails;
	}
	public void setAccountDetails(Map<String,List<String>> accountDetails) {
		this.accountDetails = accountDetails;
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
	private GenericHeader header;
	 private String token;
	 private String message;
	 private String messageCode;
		
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	 
	 
}
