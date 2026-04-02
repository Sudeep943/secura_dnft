package com.secura.dnft.request.response;

public class AddTenantRequest {
	
	private GenericHeader header;
	private String profileId;
	private String flatId;
	private String addtoExisting;
	
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	public String getProfileId() {
		return profileId;
	}
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}
	public String getFlatId() {
		return flatId;
	}
	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}
	public String getAddtoExisting() {
		return addtoExisting;
	}
	public void setAddtoExisting(String addtoExisting) {
		this.addtoExisting = addtoExisting;
	}
	
	
	
}
