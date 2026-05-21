package com.secura.dnft.request.response;

public class UploadPastDueRequest {

	private GenericHeader genericHeader;
	private String file;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
