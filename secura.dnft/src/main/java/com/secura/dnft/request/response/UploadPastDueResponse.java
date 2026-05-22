package com.secura.dnft.request.response;

public class UploadPastDueResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private String file;
	private Integer successRows;
	private Integer failedRows;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
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

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Integer getSuccessRows() {
		return successRows;
	}

	public void setSuccessRows(Integer successRows) {
		this.successRows = successRows;
	}

	public Integer getFailedRows() {
		return failedRows;
	}

	public void setFailedRows(Integer failedRows) {
		this.failedRows = failedRows;
	}
}
