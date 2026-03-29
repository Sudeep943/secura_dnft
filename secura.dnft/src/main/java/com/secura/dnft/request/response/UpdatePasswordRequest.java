package com.secura.dnft.request.response;

public class UpdatePasswordRequest {
	private String profileId;
	private String newPassword;
	private String oldPassword;
	private boolean otpVerified;
	
	
	public boolean isOtpVerified() {
		return otpVerified;
	}
	public void setOtpVerified(boolean otpVerified) {
		this.otpVerified = otpVerified;
	}
	public String getProfileId() {
		return profileId;
	}
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}
	public String getNewPassword() {
		return newPassword;
	}
	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
	public String getOldPassword() {
		return oldPassword;
	}
	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}
	
}
