package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Booking;

public class GetUpcomigBookingResponse {
	private  GenericHeader header;
	private List<Booking> approvedBookingList;
	private List<Booking> pendigBookingList;
	 private String message;
	 private String messageCode;
	
	 
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
public GenericHeader getHeader() {
	return header;
   }
public void setHeader(GenericHeader header) {
	this.header = header;
}
public List<Booking> getApprovedBookingList() {
	return approvedBookingList;
}
public void setApprovedBookingList(List<Booking> approvedBookingList) {
	this.approvedBookingList = approvedBookingList;
}
public List<Booking> getPendigBookingList() {
	return pendigBookingList;
}
public void setPendigBookingList(List<Booking> pendigBookingList) {
	this.pendigBookingList = pendigBookingList;
}

}
