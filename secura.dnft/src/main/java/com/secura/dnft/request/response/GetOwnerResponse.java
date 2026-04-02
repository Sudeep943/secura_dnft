package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Profile;

public class GetOwnerResponse {
	
	private GenericHeader genericHeader;
	private List<Profile> profile;
	private Owner owner;
	private String message;
	private String messageCode;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public List<Profile> getProfile() {
		return profile;
	}
	public void setProfile(List<Profile> profile) {
		this.profile = profile;
	}
	public Owner getOwner() {
		return owner;
	}
	public void setOwner(Owner owner) {
		this.owner = owner;
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
}
