package com.secura.dnft.request.response;

import java.util.List;
import java.util.Map;

public class GetDuePaymentAmountDetailsResponse {
	private GenericHeader genericHeader;
	private List<DueAmountDetails> listOfDueAmountDetails;
	private Map<String, List<DueAmountDetails>> flatTypeDueAmountDetails;
	private String message;
    private String messageCode;

	public List<DueAmountDetails> getListOfDueAmountDetails() {
		return listOfDueAmountDetails;
	}

	public void setListOfDueAmountDetails(List<DueAmountDetails> listOfDueAmountDetails) {
		this.listOfDueAmountDetails = listOfDueAmountDetails;
	}

	public Map<String, List<DueAmountDetails>> getFlatTypeDueAmountDetails() {
		return flatTypeDueAmountDetails;
	}

	public void setFlatTypeDueAmountDetails(Map<String, List<DueAmountDetails>> flatTypeDueAmountDetails) {
		this.flatTypeDueAmountDetails = flatTypeDueAmountDetails;
	}

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
    
    
}
