package com.secura.dnft.request.response;

import java.util.Map;

public class PaymentGayewayOrderResponse {

	
	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private RazorPayOrderResponse order;
	private Map<String, Object> data;
	
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
	public RazorPayOrderResponse getOrder() {
		return order;
	}
	public void setOrder(RazorPayOrderResponse order) {
		this.order = order;
	}
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	
}
