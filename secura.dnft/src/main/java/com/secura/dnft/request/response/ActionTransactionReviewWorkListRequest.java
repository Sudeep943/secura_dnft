package com.secura.dnft.request.response;

public class ActionTransactionReviewWorkListRequest {

	private GenericHeader genericHeader;
	private String worklistId;
	private String action;

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

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
