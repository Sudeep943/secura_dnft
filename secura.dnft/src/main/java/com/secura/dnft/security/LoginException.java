package com.secura.dnft.security;

import java.util.List;
import java.util.Map;

public class LoginException extends Exception{
	private static final long serialVersionUID = 1L;
	private String errorMessage;
	private String errorMessageCode;
	private Map<String,List<String>> accountDetails;
	
	public LoginException(String errorMessage, String errorMessageCode, Map<String,List<String>> accountDetails) {
		super();
		this.errorMessage = errorMessage;
		this.errorMessageCode = errorMessageCode;
		this.accountDetails = accountDetails;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getErrorMessageCode() {
		return errorMessageCode;
	}
	public void setErrorMessageCode(String errorMessageCode) {
		this.errorMessageCode = errorMessageCode;
	}
	public Map<String,List<String>> getAccountDetails() {
		return accountDetails;
	}
	public void setAccountDetails(Map<String,List<String>> accountDetails) {
		this.accountDetails = accountDetails;
	}
	
	
}
