package com.secura.dnft.request.response;

public class AddOwnerResponse {

	private GenericHeader header;
	private String message;
	private String messageCode;
	private String ownerId;
	private String flatId;
	
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
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public String getFlatId() {
		return flatId;
	}
	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}
	
	
}
