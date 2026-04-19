package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.secura.dnft.bean.BankAccountDetails;
import com.secura.dnft.bean.ExecutiveMember;
import com.secura.dnft.generic.bean.Address;

public class UpdateApartmentDetailsRequest {

	private GenericHeader genericHeader;
	private String apartmentLogo;
	private List<BankAccountDetails> bankAccountDetails;
	private Address address;
	@JsonProperty("excutiveMemberList")
	@JsonAlias("executiveMemberList")
	private List<ExecutiveMember> executiveMemberList;
	private String apartmentLetterHead;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getApartmentLogo() {
		return apartmentLogo;
	}

	public void setApartmentLogo(String apartmentLogo) {
		this.apartmentLogo = apartmentLogo;
	}

	public List<BankAccountDetails> getBankAccountDetails() {
		return bankAccountDetails;
	}

	public void setBankAccountDetails(List<BankAccountDetails> bankAccountDetails) {
		this.bankAccountDetails = bankAccountDetails;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public List<ExecutiveMember> getExecutiveMemberList() {
		return executiveMemberList;
	}

	public void setExecutiveMemberList(List<ExecutiveMember> executiveMemberList) {
		this.executiveMemberList = executiveMemberList;
	}

	public String getApartmentLetterHead() {
		return apartmentLetterHead;
	}

	public void setApartmentLetterHead(String apartmentLetterHead) {
		this.apartmentLetterHead = apartmentLetterHead;
	}
}
