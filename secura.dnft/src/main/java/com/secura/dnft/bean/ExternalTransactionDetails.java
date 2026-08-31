package com.secura.dnft.bean;

public class ExternalTransactionDetails {

	private String utr;
    private String referenceNumber;
    private String transactionId;
    private String rawText; // Optional: helpful for debugging OCR output

    // Getters and Setters
    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
}
