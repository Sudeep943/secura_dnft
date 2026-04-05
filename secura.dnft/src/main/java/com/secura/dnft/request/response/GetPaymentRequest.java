package com.secura.dnft.request.response;

import java.sql.Date;

public class GetPaymentRequest {
	private GenericHeader genericHeader;
	
	private String paymentAmount;
	private String gst;
	private Date collectionStartDate;
	private Date collectionEndDate;
	private String paymentCollectionCycle;
	private String paymentCollectionMode;
	private String paymentCapita;


	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public String getGst() {
		return gst;
	}

	public void setGst(String gst) {
		this.gst = gst;
	}

	public Date getCollectionStartDate() {
		return collectionStartDate;
	}

	public void setCollectionStartDate(Date collectionStartDate) {
		this.collectionStartDate = collectionStartDate;
	}

	public Date getCollectionEndDate() {
		return collectionEndDate;
	}

	public void setCollectionEndDate(Date collectionEndDate) {
		this.collectionEndDate = collectionEndDate;
	}

	public String getPaymentCollectionCycle() {
		return paymentCollectionCycle;
	}

	public void setPaymentCollectionCycle(String paymentCollectionCycle) {
		this.paymentCollectionCycle = paymentCollectionCycle;
	}

	public String getPaymentCollectionMode() {
		return paymentCollectionMode;
	}

	public void setPaymentCollectionMode(String paymentCollectionMode) {
		this.paymentCollectionMode = paymentCollectionMode;
	}

	public String getPaymentCapita() {
		return paymentCapita;
	}

	public void setPaymentCapita(String paymentCapita) {
		this.paymentCapita = paymentCapita;
	}
	
	
}
