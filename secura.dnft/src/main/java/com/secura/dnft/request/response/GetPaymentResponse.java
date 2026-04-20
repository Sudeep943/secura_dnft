package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

import com.secura.dnft.entity.PaymentEntity;

public class GetPaymentResponse {
	
	private GenericHeader genericHeader;
	private String message;
    private String messageCode;
    private Date dueDate;
    private String amount;
    private String capita;
    private List<PaymentEntity> paymentList;
    
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
	public Date getDueDate() {
		return dueDate;
	}
	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getCapita() {
		return capita;
	}
	public void setCapita(String capita) {
		this.capita = capita;
	}

	public List<PaymentEntity> getPaymentList() {
		return paymentList;
	}

	public void setPaymentList(List<PaymentEntity> paymentList) {
		this.paymentList = paymentList;
	}
     
	
     
    
}
