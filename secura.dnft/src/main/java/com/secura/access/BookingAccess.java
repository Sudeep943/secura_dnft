package com.secura.access;

public class BookingAccess {
	private boolean parentAccess;
	private boolean viewAllBookingtAccess;
	private boolean createSoceityBookingtAccess;
	private boolean manageBookingtAccess;
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isViewAllBookingtAccess() {
		return viewAllBookingtAccess;
	}
	public void setViewAllBookingtAccess(boolean viewAllBookingtAccess) {
		this.viewAllBookingtAccess = viewAllBookingtAccess;
	}
	public boolean isCreateSoceityBookingtAccess() {
		return createSoceityBookingtAccess;
	}
	public void setCreateSoceityBookingtAccess(boolean createSoceityBookingtAccess) {
		this.createSoceityBookingtAccess = createSoceityBookingtAccess;
	}
	public boolean isManageBookingtAccess() {
		return manageBookingtAccess;
	}
	public void setManageBookingtAccess(boolean manageBookingtAccess) {
		this.manageBookingtAccess = manageBookingtAccess;
	}
	


}
