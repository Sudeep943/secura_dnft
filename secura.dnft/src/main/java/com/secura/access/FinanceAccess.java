package com.secura.access;

public class FinanceAccess {
	
	private boolean parentAccess;
	private boolean ledgerEntryAccess;	
	private boolean createNewPaymentAccess;
	private boolean updatePaymentAccess;
	private boolean createReceiptAccess;
	private boolean viewAllTransactionAccess;
	private boolean uploadPastPaymentAccess;
	private boolean reconcilePaymentAccess;
	private boolean budgetManagment;
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isLedgerEntryAccess() {
		return ledgerEntryAccess;
	}
	public void setLedgerEntryAccess(boolean ledgerEntryAccess) {
		this.ledgerEntryAccess = ledgerEntryAccess;
	}
	public boolean isCreateNewPaymentAccess() {
		return createNewPaymentAccess;
	}
	public void setCreateNewPaymentAccess(boolean createNewPaymentAccess) {
		this.createNewPaymentAccess = createNewPaymentAccess;
	}
	public boolean isUpdatePaymentAccess() {
		return updatePaymentAccess;
	}
	public void setUpdatePaymentAccess(boolean updatePaymentAccess) {
		this.updatePaymentAccess = updatePaymentAccess;
	}
	public boolean isCreateReceiptAccess() {
		return createReceiptAccess;
	}
	public void setCreateReceiptAccess(boolean createReceiptAccess) {
		this.createReceiptAccess = createReceiptAccess;
	}
	public boolean isViewAllTransactionAccess() {
		return viewAllTransactionAccess;
	}
	public void setViewAllTransactionAccess(boolean viewAllTransactionAccess) {
		this.viewAllTransactionAccess = viewAllTransactionAccess;
	}
	public boolean isUploadPastPaymentAccess() {
		return uploadPastPaymentAccess;
	}
	public void setUploadPastPaymentAccess(boolean uploadPastPaymentAccess) {
		this.uploadPastPaymentAccess = uploadPastPaymentAccess;
	}
	public boolean isReconcilePaymentAccess() {
		return reconcilePaymentAccess;
	}
	public void setReconcilePaymentAccess(boolean reconcilePaymentAccess) {
		this.reconcilePaymentAccess = reconcilePaymentAccess;
	}
	public boolean isBudgetManagment() {
		return budgetManagment;
	}
	public void setBudgetManagment(boolean budgetManagment) {
		this.budgetManagment = budgetManagment;
	}


	
}
