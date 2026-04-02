package com.secura.dnft.request.response;

public class RemoveOwnerTenantProfileRequest {

	private GenericHeader header;
	private String flatId;
	private String id;
	private String profileType;

	public GenericHeader getHeader() {
		return header;
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProfileType() {
		return profileType;
	}

	public void setProfileType(String profileType) {
		this.profileType = profileType;
	}
}
