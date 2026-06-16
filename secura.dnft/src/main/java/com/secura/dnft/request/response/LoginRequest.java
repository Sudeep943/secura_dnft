package com.secura.dnft.request.response;

public class LoginRequest {

	private String username;
    private String password;
    private String otp;
   // private ProfileAccountDetails accountDetails;
    private String flatID;
    private String apartmentId;
    
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getOtp() {
		return otp;
	}
	public void setOtp(String otp) {
		this.otp = otp;
	}
	public String getFlatID() {
		return flatID;
	}
	public void setFlatID(String flatID) {
		this.flatID = flatID;
	}
	public String getApartmentId() {
		return apartmentId;
	}
	public void setApartmentId(String apartmentId) {
		this.apartmentId = apartmentId;
	}
    
}
