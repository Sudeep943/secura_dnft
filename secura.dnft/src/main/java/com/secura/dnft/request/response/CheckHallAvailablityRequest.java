package com.secura.dnft.request.response;

import java.sql.Date;

public class CheckHallAvailablityRequest {
	
	private GenericHeader genericHeader;
	private String hallId;
    private Date eventDate;
   
    
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getHallId() {
		return hallId;
	}
	public void setHallId(String hallId) {
		this.hallId = hallId;
	}
	public Date getEventDate() {
		return eventDate;
	}
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
    

}
