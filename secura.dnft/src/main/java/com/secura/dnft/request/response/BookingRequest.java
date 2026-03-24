package com.secura.dnft.request.response;

import java.sql.Date;

public class BookingRequest {
	
	
	private GenericHeader genericHeader;
	private String flatNo;
	private  Date eventDate;
	private String expectedGuest;
	private String bookingType;
	private String bookingPurpose;
	private String bookingHallId;
	private String bookingHallName;
	private String bookingTransactionId;
	private String tender;
	private String hallName;
	private String amountPaid;
	
	
	public String getHallName() {
		return hallName;
	}
	public void setHallName(String hallName) {
		this.hallName = hallName;
	}
	public String getBookingHallId() {
		return bookingHallId;
	}
	public void setBookingHallId(String bookingHallId) {
		this.bookingHallId = bookingHallId;
	}
	public String getBookingHallName() {
		return bookingHallName;
	}
	public void setBookingHallName(String bookingHallName) {
		this.bookingHallName = bookingHallName;
	}
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getFlatNo() {
		return flatNo;
	}
	public void setFlatNo(String flatNo) {
		this.flatNo = flatNo;
	}
	public Date getEventDate() {
		return eventDate;
	}
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
	public String getExpectedGuest() {
		return expectedGuest;
	}
	public void setExpectedGuest(String expectedGuest) {
		this.expectedGuest = expectedGuest;
	}
	public String getBookingType() {
		return bookingType;
	}
	public void setBookingType(String bookingyType) {
		this.bookingType = bookingyType;
	}
	public String getBookingPurpose() {
		return bookingPurpose;
	}
	public void setBookingPurpose(String bookingPurpose) {
		this.bookingPurpose = bookingPurpose;
	}
	public String getBookingTransactionId() {
		return bookingTransactionId;
	}
	public void setBookingTransactionId(String bookingTransactionId) {
		this.bookingTransactionId = bookingTransactionId;
	}
	public String getTender() {
		return tender;
	}
	public void setTender(String tender) {
		this.tender = tender;
	}
	public String getAmountPaid() {
		return amountPaid;
	}
	public void setAmountPaid(String amountPaid) {
		this.amountPaid = amountPaid;
	}
	
	
	
	

}
