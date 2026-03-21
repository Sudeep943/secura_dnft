package com.secura.dnft.security;

public class BusinessException extends Exception{

	private static final long serialVersionUID = 1L;
	private String errorMessage;
	private String errorMessageCode;
	
	
	public BusinessException(String errorMessage, String errorMessageCode) {
		super();
		this.errorMessage = errorMessage;
		this.errorMessageCode = errorMessageCode;
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
	
	
	
}
