package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.DiscFin;

public class GetDiscfinResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private List<DiscFin> discFinList;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
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

	public List<DiscFin> getDiscFinList() {
		return discFinList;
	}

	public void setDiscFinList(List<DiscFin> discFinList) {
		this.discFinList = discFinList;
	}
}
