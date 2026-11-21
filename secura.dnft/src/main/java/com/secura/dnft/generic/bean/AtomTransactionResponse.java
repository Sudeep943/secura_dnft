package com.secura.dnft.generic.bean;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AtomTransactionResponse {

    private PayInstrument payInstrument;

    public PayInstrument getPayInstrument() {
        return payInstrument;
    }

    public void setPayInstrument(PayInstrument payInstrument) {
        this.payInstrument = payInstrument;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayInstrument {

        private MerchDetails merchDetails;
        private PayDetails payDetails;
        private PayModeSpecificData payModeSpecificData;
        private Extras extras;
        private CustDetails custDetails;
        private ResponseDetails responseDetails;

        public MerchDetails getMerchDetails() { return merchDetails; }
        public void setMerchDetails(MerchDetails value) { this.merchDetails = value; }

        public PayDetails getPayDetails() { return payDetails; }
        public void setPayDetails(PayDetails value) { this.payDetails = value; }

        public PayModeSpecificData getPayModeSpecificData() { return payModeSpecificData; }
        public void setPayModeSpecificData(PayModeSpecificData value) { this.payModeSpecificData = value; }

        public Extras getExtras() { return extras; }
        public void setExtras(Extras value) { this.extras = value; }

        public CustDetails getCustDetails() { return custDetails; }
        public void setCustDetails(CustDetails value) { this.custDetails = value; }

        public ResponseDetails getResponseDetails() { return responseDetails; }
        public void setResponseDetails(ResponseDetails value) { this.responseDetails = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchDetails {

        private Long merchId;
        private String merchTxnId;
        private String merchTxnDate;

        public Long getMerchId() { return merchId; }
        public void setMerchId(Long value) { this.merchId = value; }

        public String getMerchTxnId() { return merchTxnId; }
        public void setMerchTxnId(String value) { this.merchTxnId = value; }

        public String getMerchTxnDate() { return merchTxnDate; }
        public void setMerchTxnDate(String value) { this.merchTxnDate = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayDetails {

        private Long atomTxnId;
        private List<ProdDetails> prodDetails;
        private BigDecimal amount;
        private BigDecimal surchargeAmount;
        private BigDecimal totalAmount;
        private String custAccNo;
        private String clientCode;
        private String txnCurrency;
        private String signature;
        private String txnInitDate;
        private String txnCompleteDate;

        public Long getAtomTxnId() { return atomTxnId; }
        public void setAtomTxnId(Long value) { this.atomTxnId = value; }

        public List<ProdDetails> getProdDetails() { return prodDetails; }
        public void setProdDetails(List<ProdDetails> value) { this.prodDetails = value; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { this.amount = value; }

        public BigDecimal getSurchargeAmount() { return surchargeAmount; }
        public void setSurchargeAmount(BigDecimal value) { this.surchargeAmount = value; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal value) { this.totalAmount = value; }

        public String getCustAccNo() { return custAccNo; }
        public void setCustAccNo(String value) { this.custAccNo = value; }

        public String getClientCode() { return clientCode; }
        public void setClientCode(String value) { this.clientCode = value; }

        public String getTxnCurrency() { return txnCurrency; }
        public void setTxnCurrency(String value) { this.txnCurrency = value; }

        public String getSignature() { return signature; }
        public void setSignature(String value) { this.signature = value; }

        public String getTxnInitDate() { return txnInitDate; }
        public void setTxnInitDate(String value) { this.txnInitDate = value; }

        public String getTxnCompleteDate() { return txnCompleteDate; }
        public void setTxnCompleteDate(String value) { this.txnCompleteDate = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProdDetails {

        private String prodName;
        private BigDecimal prodAmount;

        public String getProdName() { return prodName; }
        public void setProdName(String value) { this.prodName = value; }

        public BigDecimal getProdAmount() { return prodAmount; }
        public void setProdAmount(BigDecimal value) { this.prodAmount = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayModeSpecificData {

        private List<String> subChannel;
        private BankDetails bankDetails;

        public List<String> getSubChannel() { return subChannel; }
        public void setSubChannel(List<String> value) { this.subChannel = value; }

        public BankDetails getBankDetails() { return bankDetails; }
        public void setBankDetails(BankDetails value) { this.bankDetails = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankDetails {

        private Integer otsBankId;
        private String bankTxnId;
        private String authId;
        private String otsBankName;
        private String cardType;
        private String cardMaskNumber;
        private String scheme;

        public Integer getOtsBankId() { return otsBankId; }
        public void setOtsBankId(Integer value) { this.otsBankId = value; }

        public String getBankTxnId() { return bankTxnId; }
        public void setBankTxnId(String value) { this.bankTxnId = value; }

        public String getAuthId() { return authId; }
        public void setAuthId(String value) { this.authId = value; }

        public String getOtsBankName() { return otsBankName; }
        public void setOtsBankName(String value) { this.otsBankName = value; }

        public String getCardType() { return cardType; }
        public void setCardType(String value) { this.cardType = value; }

        public String getCardMaskNumber() { return cardMaskNumber; }
        public void setCardMaskNumber(String value) { this.cardMaskNumber = value; }

        public String getScheme() { return scheme; }
        public void setScheme(String value) { this.scheme = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extras {

        private String udf1;
        private String udf2;

        public String getUdf1() { return udf1; }
        public void setUdf1(String value) { this.udf1 = value; }

        public String getUdf2() { return udf2; }
        public void setUdf2(String value) { this.udf2 = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustDetails {

        private String custEmail;
        private String custMobile;
        private Map<String, Object> billingInfo;

        public String getCustEmail() { return custEmail; }
        public void setCustEmail(String value) { this.custEmail = value; }

        public String getCustMobile() { return custMobile; }
        public void setCustMobile(String value) { this.custMobile = value; }

        public Map<String, Object> getBillingInfo() { return billingInfo; }
        public void setBillingInfo(Map<String, Object> value) { this.billingInfo = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseDetails {

        private String statusCode;
        private String message;
        private String description;

        public String getStatusCode() { return statusCode; }
        public void setStatusCode(String value) { this.statusCode = value; }

        public String getMessage() { return message; }
        public void setMessage(String value) { this.message = value; }

        public String getDescription() { return description; }
        public void setDescription(String value) { this.description = value; }
    }
}

