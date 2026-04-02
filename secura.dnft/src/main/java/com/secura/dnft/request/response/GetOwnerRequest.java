package com.secura.dnft.request.response;

public class GetOwnerRequest {

	private GenericHeader genericHeader;
	private String flatId;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getFlatId() {
		return flatId;
	}
	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}
}
