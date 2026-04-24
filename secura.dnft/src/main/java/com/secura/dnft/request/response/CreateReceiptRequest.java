package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateReceiptRequest {

	private GenericHeader genericHeader;
	private List<Items> items;
	private List<AddedCharges> addedCharges;

	@JsonAlias({ "disFinObject", "discFinObject", "disFInObject", "discFinReceipt" })
	private DiscFinReceipt discFinReceipt;

	private String receiptType;
	private boolean perheadFlag;
	private String remarks;
	private boolean unitPriceRequired;
	private String totalAmount;
	private String transactionId;
	private String flatId;
	@JsonAlias({ "tenderList" })
	private List<PaymentTenderData> paymentTenderDataList;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<Items> getItems() {
		return items;
	}

	public void setItems(List<Items> items) {
		this.items = items;
	}

	public List<AddedCharges> getAddedCharges() {
		return addedCharges;
	}

	public void setAddedCharges(List<AddedCharges> addedCharges) {
		this.addedCharges = addedCharges;
	}

	public DiscFinReceipt getDiscFinReceipt() {
		return discFinReceipt;
	}

	public void setDiscFinReceipt(DiscFinReceipt discFinReceipt) {
		this.discFinReceipt = discFinReceipt;
	}

	public String getReceiptType() {
		return receiptType;
	}

	public void setReceiptType(String receiptType) {
		this.receiptType = receiptType;
	}

	public boolean isPerheadFlag() {
		return perheadFlag;
	}

	public void setPerheadFlag(boolean perheadFlag) {
		this.perheadFlag = perheadFlag;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public boolean isUnitPriceRequired() {
		return unitPriceRequired;
	}

	public void setUnitPriceRequired(boolean unitPriceRequired) {
		this.unitPriceRequired = unitPriceRequired;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public List<PaymentTenderData> getPaymentTenderDataList() {
		return paymentTenderDataList;
	}

	public void setPaymentTenderDataList(List<PaymentTenderData> paymentTenderDataList) {
		this.paymentTenderDataList = paymentTenderDataList;
	}
}
