package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Tenant;

public class GetTenantResponse {
	
	private GenericHeader genericHeader;
	private List<Profile> profile;
	private Tenant tenant;
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
	public Tenant getTenant() {
		return tenant;
	}
	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
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
