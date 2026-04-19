package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.secura.dnft.bean.BankAccountDetails;
import com.secura.dnft.bean.ExecutiveMember;
import com.secura.dnft.generic.bean.Address;

public class GetApartmentDetailsResponse {

	private GenericHeader genericHeader;
	private String apartmentName;
	private String apartmentLogo;
	private List<BankAccountDetails> bankAccountDetails;
	private Address address;
	@JsonProperty("excutiveMemberList")
	@JsonAlias("executiveMemberList")
	private List<ExecutiveMember> executiveMemberList;
	private String apartmentLetterHead;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getApartmentName() {
		return apartmentName;
	}

	public void setApartmentName(String apartmentName) {
		this.apartmentName = apartmentName;
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
}
