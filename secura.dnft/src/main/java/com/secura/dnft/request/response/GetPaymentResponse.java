package com.secura.dnft.request.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class GetPaymentResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private String paymentId;
	private String paymentName;
	private String shortDetails;
	private String paymentCapita;
	private String paymentAmount;
	private String gst;
	private String currency;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueDate;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate collectionStartDate;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate collectionEndDate;
	private List<String> paymentCollectionCycleList;
	private String paymentCollectionMode;
	private boolean partialPaymentAllowed;
	private String applicableFor;
	private String allowedPaymentModes;
	private String paidFlats;
	private String paymentType;
	private String bankAccountId;
	private String status;
	private String aprmtId;
	private String causeId;
	private String addedCharges;
	private String discFin;
	private LocalDateTime creatTs;
	private String creatUsrId;
	private LocalDateTime lstUpdtTs;
	private String lstUpdtUsrId;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}

	public String getShortDetails() {
		return shortDetails;
	}

	public void setShortDetails(String shortDetails) {
		this.shortDetails = shortDetails;
	}

	public String getPaymentCapita() {
		return paymentCapita;
	}

	public void setPaymentCapita(String paymentCapita) {
		this.paymentCapita = paymentCapita;
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

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getCollectionStartDate() {
		return collectionStartDate;
	}

	public void setCollectionStartDate(LocalDate collectionStartDate) {
		this.collectionStartDate = collectionStartDate;
	}

	public LocalDate getCollectionEndDate() {
		return collectionEndDate;
	}

	public void setCollectionEndDate(LocalDate collectionEndDate) {
		this.collectionEndDate = collectionEndDate;
	}

	public List<String> getPaymentCollectionCycleList() {
		return paymentCollectionCycleList;
	}

	public void setPaymentCollectionCycleList(List<String> paymentCollectionCycleList) {
		this.paymentCollectionCycleList = paymentCollectionCycleList;
	}

	public String getPaymentCollectionMode() {
		return paymentCollectionMode;
	}

	public void setPaymentCollectionMode(String paymentCollectionMode) {
		this.paymentCollectionMode = paymentCollectionMode;
	}

	public boolean isPartialPaymentAllowed() {
		return partialPaymentAllowed;
	}

	public void setPartialPaymentAllowed(boolean partialPaymentAllowed) {
		this.partialPaymentAllowed = partialPaymentAllowed;
	}

	public String getApplicableFor() {
		return applicableFor;
	}

	public void setApplicableFor(String applicableFor) {
		this.applicableFor = applicableFor;
	}

	public String getAllowedPaymentModes() {
		return allowedPaymentModes;
	}

	public void setAllowedPaymentModes(String allowedPaymentModes) {
		this.allowedPaymentModes = allowedPaymentModes;
	}

	public String getPaidFlats() {
		return paidFlats;
	}

	public void setPaidFlats(String paidFlats) {
		this.paidFlats = paidFlats;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getBankAccountId() {
		return bankAccountId;
	}

	public void setBankAccountId(String bankAccountId) {
		this.bankAccountId = bankAccountId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAprmtId() {
		return aprmtId;
	}

	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}

	public String getCauseId() {
		return causeId;
	}

	public void setCauseId(String causeId) {
		this.causeId = causeId;
	}

	public String getAddedCharges() {
		return addedCharges;
	}

	public void setAddedCharges(String addedCharges) {
		this.addedCharges = addedCharges;
	}

	public String getDiscFin() {
		return discFin;
	}

	public void setDiscFin(String discFin) {
		this.discFin = discFin;
	}

	public LocalDateTime getCreatTs() {
		return creatTs;
	}

	public void setCreatTs(LocalDateTime creatTs) {
		this.creatTs = creatTs;
	}

	public String getCreatUsrId() {
		return creatUsrId;
	}

	public void setCreatUsrId(String creatUsrId) {
		this.creatUsrId = creatUsrId;
	}

	public LocalDateTime getLstUpdtTs() {
		return lstUpdtTs;
	}

	public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
		this.lstUpdtTs = lstUpdtTs;
	}

	public String getLstUpdtUsrId() {
		return lstUpdtUsrId;
	}

	public void setLstUpdtUsrId(String lstUpdtUsrId) {
		this.lstUpdtUsrId = lstUpdtUsrId;
	}
}
