package com.secura.access;

public class VendorManagementAccess {
	private boolean parentAccess;
	private boolean addNewVendorAccess;
	private boolean updateVendorAccess;
	private boolean viewVendorsAccess;
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isAddNewVendorAccess() {
		return addNewVendorAccess;
	}
	public void setAddNewVendorAccess(boolean addNewVendorAccess) {
		this.addNewVendorAccess = addNewVendorAccess;
	}
	public boolean isUpdateVendorAccess() {
		return updateVendorAccess;
	}
	public void setUpdateVendorAccess(boolean updateVendorAccess) {
		this.updateVendorAccess = updateVendorAccess;
	}
	public boolean isViewVendorsAccess() {
		return viewVendorsAccess;
	}
	public void setViewVendorsAccess(boolean viewVendorsAccess) {
		this.viewVendorsAccess = viewVendorsAccess;
	}
	
	
	
	
}
