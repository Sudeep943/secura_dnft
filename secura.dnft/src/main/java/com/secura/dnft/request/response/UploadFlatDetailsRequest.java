package com.secura.dnft.request.response;

public class UploadFlatDetailsRequest {

	private GenericHeader header;
	private String documentName;
	private String documentCode;
	private String importType;
	private String sheetName;
	private String documentData;

	public GenericHeader getHeader() {
		return header;
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getDocumentCode() {
		return documentCode;
	}

	public void setDocumentCode(String documentCode) {
		this.documentCode = documentCode;
	}

	public String getImportType() {
		return importType;
	}

	public void setImportType(String importType) {
		this.importType = importType;
	}

	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	public String getDocumentData() {
		return documentData;
	}

	public void setDocumentData(String documentData) {
		this.documentData = documentData;
	}
}
