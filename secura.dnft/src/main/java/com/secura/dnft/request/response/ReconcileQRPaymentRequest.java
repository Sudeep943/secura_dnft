package com.secura.dnft.request.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;

public class ReconcileQRPaymentRequest {

	private GenericHeader genericHeader;
	private LocalDate fromDate;
	@JsonAlias({ "toDate", "todate" })
	private LocalDate toDate;
	@JsonAlias({ "base64EncodedStatementFile" })
	private String base64EncodedSatementFile;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public LocalDate getFromDate() {
		return fromDate;
	}

	public void setFromDate(LocalDate fromDate) {
		this.fromDate = fromDate;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public void setToDate(LocalDate toDate) {
		this.toDate = toDate;
	}

	public String getBase64EncodedSatementFile() {
		return base64EncodedSatementFile;
	}

	public void setBase64EncodedSatementFile(String base64EncodedSatementFile) {
		this.base64EncodedSatementFile = base64EncodedSatementFile;
	}
}
