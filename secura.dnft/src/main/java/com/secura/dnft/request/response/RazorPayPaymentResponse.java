package com.secura.dnft.request.response;

public class RazorPayPaymentResponse {

	
	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private RazorPayOrderResponse order;
	
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
	
	
}
