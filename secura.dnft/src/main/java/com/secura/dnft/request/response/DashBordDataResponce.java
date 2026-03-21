package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Booking;

public class DashBordDataResponce {
	private GenericHeader header;
	
	private List<Booking> upcomingBookings;
	private long pendingWorklistCount;
	
	public List<Booking> getUpcomingBookings() {
		return upcomingBookings;
	}
	public void setUpcomingBookings(List<Booking> upcomingBookings) {
		this.upcomingBookings = upcomingBookings;
	}
	public long getPendingWorklistCount() {
		return pendingWorklistCount;
	}
	public void setPendingWorklistCount(long pendingWorklistCount) {
		this.pendingWorklistCount = pendingWorklistCount;
	}
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	
	
}
