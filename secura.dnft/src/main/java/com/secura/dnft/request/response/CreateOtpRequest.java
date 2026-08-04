package com.secura.dnft.request.response;

import java.util.List;

public class CreateOtpRequest {

	private GenericHeader genericHeader;
	private String userId;
	private List<String> userIds;
	private boolean sendMobile;
	private boolean sendMail;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getUserIds() {
		return userIds;
	}

	public void setUserIds(List<String> userIds) {
		this.userIds = userIds;
	}

	public boolean isSendMobile() {
		return sendMobile;
	}

	public void setSendMobile(boolean sendMobile) {
		this.sendMobile = sendMobile;
	}

	public boolean isSendMail() {
		return sendMail;
	}

	public void setSendMail(boolean sendMail) {
		this.sendMail = sendMail;
	}
}
