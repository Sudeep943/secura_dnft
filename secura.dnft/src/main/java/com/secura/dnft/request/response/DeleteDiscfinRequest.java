package com.secura.dnft.request.response;

public class DeleteDiscfinRequest {

	private GenericHeader genericHeader;
	private String discFnId;
	private String discFinId;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getDiscFnId() {
		return discFnId;
	}

	public void setDiscFnId(String discFnId) {
		this.discFnId = discFnId;
	}

	public String getDiscFinId() {
		return discFinId;
	}

	public void setDiscFinId(String discFinId) {
		this.discFinId = discFinId;
	}
}
