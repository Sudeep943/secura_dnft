package com.secura.access;

public class AccountManagmentAccess {

	private boolean parentAccess;
	private  boolean createUpdateProfileAccess;
	private boolean viewAllProfile;
	private  boolean allTenantManagement;
	private  boolean allOwnerManagement;
    
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isCreateUpdateProfileAccess() {
		return createUpdateProfileAccess;
	}
	public void setCreateUpdateProfileAccess(boolean createUpdateProfileAccess) {
		this.createUpdateProfileAccess = createUpdateProfileAccess;
	}
	public boolean isViewAllProfile() {
		return viewAllProfile;
	}
	public void setViewAllProfile(boolean viewAllProfile) {
		this.viewAllProfile = viewAllProfile;
	}
	public boolean isAllTenantManagement() {
		return allTenantManagement;
	}
	public void setAllTenantManagement(boolean allTenantManagement) {
		this.allTenantManagement = allTenantManagement;
	}
	public boolean isAllOwnerManagement() {
		return allOwnerManagement;
	}
	public void setAllOwnerManagement(boolean allOwnerManagement) {
		this.allOwnerManagement = allOwnerManagement;
	}
   
    
}
