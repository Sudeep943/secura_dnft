package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.SocietyCollectionTypes;

public class GetSocirtyCollectionTypesResponse {

	private String message;
	private String messageCode;
	private List<SocietyCollectionTypes> socirtyCollectionTypes;

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

	public List<SocietyCollectionTypes> getSocirtyCollectionTypes() {
		return socirtyCollectionTypes;
	}

	public void setSocirtyCollectionTypes(List<SocietyCollectionTypes> socirtyCollectionTypes) {
		this.socirtyCollectionTypes = socirtyCollectionTypes;
	}
}
