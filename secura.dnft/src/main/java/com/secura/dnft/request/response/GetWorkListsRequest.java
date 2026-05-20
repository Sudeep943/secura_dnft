package com.secura.dnft.request.response;

public class GetWorkListsRequest {

	private GenericHeader genericHeader;
	private String worklistId;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getWorklistId() {
		return worklistId;
	}

	public void setWorklistId(String worklistId) {
		this.worklistId = worklistId;
	}
}
