package com.secura.dnft.request.response;

import java.time.LocalDate;

public class DuePaymentAmountDetailsRequest {

    private GenericHeader genericHeader;
    private String paymentAmount;
    private String gst;
    private LocalDate collectionStartDate;
    private LocalDate collectionEndDate;
    private String paymentCollectionCycle;
    private String paymentCollectionMode;
    private String paymentCapita;
    private LocalDate todayDate;

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

    public LocalDate getTodayDate() {
        return todayDate;
    }

    public void setTodayDate(LocalDate todayDate) {
        this.todayDate = todayDate;
    }
}
