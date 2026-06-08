package com.secura.access;

public class TicketManagementAccess {
	private boolean parentAccess;
	private boolean viewAllTicketAccess;
	private boolean reAsignTicketAccess;
	private boolean ticketHeadAccess;
	
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isViewAllTicketAccess() {
		return viewAllTicketAccess;
	}
	public void setViewAllTicketAccess(boolean viewAllTicketAccess) {
		this.viewAllTicketAccess = viewAllTicketAccess;
	}
	public boolean isReAsignTicketAccess() {
		return reAsignTicketAccess;
	}
	public void setReAsignTicketAccess(boolean reAsignTicketAccess) {
		this.reAsignTicketAccess = reAsignTicketAccess;
	}
	public boolean isTicketHeadAccess() {
		return ticketHeadAccess;
	}
	public void setTicketHeadAccess(boolean ticketHeadAccess) {
		this.ticketHeadAccess = ticketHeadAccess;
	}
	
	
}
