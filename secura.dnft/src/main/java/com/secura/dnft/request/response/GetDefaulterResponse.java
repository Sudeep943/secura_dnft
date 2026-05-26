package com.secura.dnft.request.response;

import java.util.List;

public class GetDefaulterResponse {

	private GenericHeader genericHeader;
	private List<Defaulter> defaulterList;
	private Integer totalDefaulters;
	private String totalMoneyCollected;
	private String totalExpectedToBeCollect;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<Defaulter> getDefaulterList() {
		return defaulterList;
	}

	public void setDefaulterList(List<Defaulter> defaulterList) {
		this.defaulterList = defaulterList;
	}

	public Integer getTotalDefaulters() {
		return totalDefaulters;
	}

	public void setTotalDefaulters(Integer totalDefaulters) {
		this.totalDefaulters = totalDefaulters;
	}

	public String getTotalMoneyCollected() {
		return totalMoneyCollected;
	}

	public void setTotalMoneyCollected(String totalMoneyCollected) {
		this.totalMoneyCollected = totalMoneyCollected;
	}

	public String getTotalExpectedToBeCollect() {
		return totalExpectedToBeCollect;
	}

	public void setTotalExpectedToBeCollect(String totalExpectedToBeCollect) {
		this.totalExpectedToBeCollect = totalExpectedToBeCollect;
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
