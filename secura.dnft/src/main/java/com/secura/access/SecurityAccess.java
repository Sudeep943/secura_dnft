package com.secura.access;

public class SecurityAccess {
	
	private boolean parentAccess;
	private boolean createAllFlatEntryAccess;
	private boolean createDailyVisitorEntryAccess;
	private boolean createVehiclePass;
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isCreateAllFlatEntryAccess() {
		return createAllFlatEntryAccess;
	}
	public void setCreateAllFlatEntryAccess(boolean createAllFlatEntryAccess) {
		this.createAllFlatEntryAccess = createAllFlatEntryAccess;
	}
	public boolean isCreateDailyVisitorEntryAccess() {
		return createDailyVisitorEntryAccess;
	}
	public void setCreateDailyVisitorEntryAccess(boolean createDailyVisitorEntryAccess) {
		this.createDailyVisitorEntryAccess = createDailyVisitorEntryAccess;
	}
	public boolean isCreateVehiclePass() {
		return createVehiclePass;
	}
	public void setCreateVehiclePass(boolean createVehiclePass) {
		this.createVehiclePass = createVehiclePass;
	}
	

}
