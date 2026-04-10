package com.secura.dnft.request.response;

public class UploadFlatDetailsResponse {

	private GenericHeader header;
	private String message;
	private String messageCode;
	private Integer totalRows;
	private Integer successRows;
	private Integer failedRows;
	private String failedRowsReportDocument;
	private String failedRowsReportDocumentName;

	public GenericHeader getHeader() {
		return header;
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
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

	public Integer getTotalRows() {
		return totalRows;
	}

	public void setTotalRows(Integer totalRows) {
		this.totalRows = totalRows;
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

	public String getFailedRowsReportDocument() {
		return failedRowsReportDocument;
	}

	public void setFailedRowsReportDocument(String failedRowsReportDocument) {
		this.failedRowsReportDocument = failedRowsReportDocument;
	}

	public String getFailedRowsReportDocumentName() {
		return failedRowsReportDocumentName;
	}

	public void setFailedRowsReportDocumentName(String failedRowsReportDocumentName) {
		this.failedRowsReportDocumentName = failedRowsReportDocumentName;
	}
}
