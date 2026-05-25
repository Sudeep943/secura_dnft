package com.secura.dnft.request.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public class GetContactDetailsRequest {

	private GenericHeader genericHeader;
	@JsonAlias("apartmentId")
	private String apartmentID;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getApartmentID() {
		return apartmentID;
	}

	public void setApartmentID(String apartmentID) {
		this.apartmentID = apartmentID;
	}
}
