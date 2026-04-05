package com.secura.dnft.request.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DuePaymentAmountDetailsResponse {

    private GenericHeader genericHeader;
    @JsonFormat(pattern = "d-MMM-yyyy")
    private LocalDate dueDate;
    private String paymentCapita;
    private String amountExcludingGst;
    private String gstPercent;
    private String gstAmount;
    private String amountIncludingGst;
    private String message;
    private String messageCode;
    
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

	public GenericHeader getGenericHeader() {
        return genericHeader;
    }

    public void setGenericHeader(GenericHeader genericHeader) {
        this.genericHeader = genericHeader;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPaymentCapita() {
        return paymentCapita;
    }

    public void setPaymentCapita(String paymentCapita) {
        this.paymentCapita = paymentCapita;
    }

    public String getAmountExcludingGst() {
        return amountExcludingGst;
    }

    public void setAmountExcludingGst(String amountExcludingGst) {
        this.amountExcludingGst = amountExcludingGst;
    }

    public String getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(String gstPercent) {
        this.gstPercent = gstPercent;
    }

    public String getAmountIncludingGst() {
        return amountIncludingGst;
    }

    public void setAmountIncludingGst(String amountIncludingGst) {
        this.amountIncludingGst = amountIncludingGst;
    }

    public String getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(String gstAmount) {
        this.gstAmount = gstAmount;
    }
}
