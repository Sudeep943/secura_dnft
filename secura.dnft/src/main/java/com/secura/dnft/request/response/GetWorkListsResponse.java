package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Worklist;

public class GetWorkListsResponse {

	private String message;
	private String messageCode;
	private List<Worklist> worklists;

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

	public List<Worklist> getWorklists() {
		return worklists;
	}

	public void setWorklists(List<Worklist> worklists) {
		this.worklists = worklists;
	}
}
