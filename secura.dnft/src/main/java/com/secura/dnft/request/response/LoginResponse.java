package com.secura.dnft.request.response;

public class LoginResponse {

	 private GenericHeader header;
	 private String token;
	 
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
