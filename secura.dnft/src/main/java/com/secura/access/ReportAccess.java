package com.secura.access;

public class ReportAccess {
	private boolean parentAccess;
	private boolean viewTotalsAccess;
	private boolean dashboardAccess;
	private boolean balanceSheetAccess;
	private boolean taxSheetAccess;
	private boolean paymentWiseCollectionAccess;
	private boolean defaulterReportAccess;
	private boolean penaltyReportAccess;
	
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isViewTotalsAccess() {
		return viewTotalsAccess;
	}
	public void setViewTotalsAccess(boolean viewTotalsAccess) {
		this.viewTotalsAccess = viewTotalsAccess;
	}
	public boolean isDashboardAccess() {
		return dashboardAccess;
	}
	public void setDashboardAccess(boolean dashboardAccess) {
		this.dashboardAccess = dashboardAccess;
	}
	public boolean isBalanceSheetAccess() {
		return balanceSheetAccess;
	}
	public void setBalanceSheetAccess(boolean balanceSheetAccess) {
		this.balanceSheetAccess = balanceSheetAccess;
	}
	public boolean isTaxSheetAccess() {
		return taxSheetAccess;
	}
	public void setTaxSheetAccess(boolean taxSheetAccess) {
		this.taxSheetAccess = taxSheetAccess;
	}
	public boolean isPaymentWiseCollectionAccess() {
		return paymentWiseCollectionAccess;
	}
	public void setPaymentWiseCollectionAccess(boolean paymentWiseCollectionAccess) {
		this.paymentWiseCollectionAccess = paymentWiseCollectionAccess;
	}
	public boolean isDefaulterReportAccess() {
		return defaulterReportAccess;
	}
	public void setDefaulterReportAccess(boolean defaulterReportAccess) {
		this.defaulterReportAccess = defaulterReportAccess;
	}
	public boolean isPenaltyReportAccess() {
		return penaltyReportAccess;
	}
	public void setPenaltyReportAccess(boolean penaltyReportAccess) {
		this.penaltyReportAccess = penaltyReportAccess;
	}
	
	
}
