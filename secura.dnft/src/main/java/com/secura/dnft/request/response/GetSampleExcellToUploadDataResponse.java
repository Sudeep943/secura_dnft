package com.secura.dnft.request.response;

public class GetSampleExcellToUploadDataResponse {

	private String message;
	private String messageCode;
	private String sampleDocumentData;
	private String sampleDocumentName;

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

	public String getSampleDocumentData() {
		return sampleDocumentData;
	}

	public void setSampleDocumentData(String sampleDocumentData) {
		this.sampleDocumentData = sampleDocumentData;
	}

	public String getSampleDocumentName() {
		return sampleDocumentName;
	}

	public void setSampleDocumentName(String sampleDocumentName) {
		this.sampleDocumentName = sampleDocumentName;
	}
}
