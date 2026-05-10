package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.SocirtyCollectionTypes;

public class GetSocirtyCollectionTypesResponse {

	private String message;
	private String messageCode;
	private List<SocirtyCollectionTypes> socirtyCollectionTypes;

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

	public List<SocirtyCollectionTypes> getSocirtyCollectionTypes() {
		return socirtyCollectionTypes;
	}

	public void setSocirtyCollectionTypes(List<SocirtyCollectionTypes> socirtyCollectionTypes) {
		this.socirtyCollectionTypes = socirtyCollectionTypes;
	}
}
