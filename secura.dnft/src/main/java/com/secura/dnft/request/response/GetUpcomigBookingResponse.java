package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Booking;

public class GetUpcomigBookingResponse {
	private  GenericHeader header;
	private List<Booking> bookingList;
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
public List<Booking> getBookingList() {
	return bookingList;
}
public void setBookingList(List<Booking> bookingList) {
	this.bookingList = bookingList;
}


}
