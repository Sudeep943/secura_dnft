package com.secura.dnft.generic.bean;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AtomAuthRequest {

    @JsonProperty("payInstrument")
    private PayInstrument payInstrument;

    public PayInstrument getPayInstrument() {
        return payInstrument;
    }

    public void setPayInstrument(PayInstrument payInstrument) {
        this.payInstrument = payInstrument;
    }

    public static class PayInstrument {

        private HeadDetails headDetails;
        private MerchDetails merchDetails;
        private PayDetails payDetails;
        private CustDetails custDetails;
        private Extras extras;
        private PayModeSpecificData payModeSpecificData;

        public PayModeSpecificData getPayModeSpecificData() {
			return payModeSpecificData;
		}
		public void setPayModeSpecificData(PayModeSpecificData payModeSpecificData) {
			this.payModeSpecificData = payModeSpecificData;
		}
		public HeadDetails getHeadDetails() { return headDetails; }
        public void setHeadDetails(HeadDetails value) { this.headDetails = value; }

        public MerchDetails getMerchDetails() { return merchDetails; }
        public void setMerchDetails(MerchDetails value) { this.merchDetails = value; }

        public PayDetails getPayDetails() { return payDetails; }
        public void setPayDetails(PayDetails value) { this.payDetails = value; }

        public CustDetails getCustDetails() { return custDetails; }
        public void setCustDetails(CustDetails value) { this.custDetails = value; }

        public Extras getExtras() { return extras; }
        public void setExtras(Extras value) { this.extras = value; }
    }

    public static class HeadDetails {

        private String version;
        private String api;
        private String platform;

        public String getVersion() { return version; }
        public void setVersion(String value) { this.version = value; }

        public String getApi() { return api; }
        public void setApi(String value) { this.api = value; }

        public String getPlatform() { return platform; }
        public void setPlatform(String value) { this.platform = value; }
    }

    public static class MerchDetails {

        private String merchId;
        private String userId;
        private String password;
        private String merchTxnId;
        private String merchTxnDate;

        public String getMerchId() { return merchId; }
        public void setMerchId(String value) { this.merchId = value; }

        public String getUserId() { return userId; }
        public void setUserId(String value) { this.userId = value; }

        public String getPassword() { return password; }
        public void setPassword(String value) { this.password = value; }

        public String getMerchTxnId() { return merchTxnId; }
        public void setMerchTxnId(String value) { this.merchTxnId = value; }

        public String getMerchTxnDate() { return merchTxnDate; }
        public void setMerchTxnDate(String value) { this.merchTxnDate = value; }
    }

    public static class PayDetails {

        private String amount;
        private String product;
        private String txnCurrency;

        public String getAmount() { return amount; }
        public void setAmount(String value) { this.amount = value; }

        public String getProduct() { return product; }
        public void setProduct(String value) { this.product = value; }

        public String getTxnCurrency() { return txnCurrency; }
        public void setTxnCurrency(String value) { this.txnCurrency = value; }
    }

    public static class CustDetails {

        private String custEmail;
        private String custMobile;

        public String getCustEmail() { return custEmail; }
        public void setCustEmail(String value) { this.custEmail = value; }

        public String getCustMobile() { return custMobile; }
        public void setCustMobile(String value) { this.custMobile = value; }
    }
    
    public static class PayModeSpecificData {

        private List<String> subChannel;

        public List<String> getSubChannel() {
            return subChannel;
        }

        public void setSubChannel(List<String> subChannel) {
            this.subChannel = subChannel;
        }
    }

    public static class Extras {

        private String udf1;
        private String udf2;
        private String udf3;

        public String getUdf1() { return udf1; }
        public void setUdf1(String value) { this.udf1 = value; }

        public String getUdf2() { return udf2; }
        public void setUdf2(String value) { this.udf2 = value; }

        public String getUdf3() { return udf3; }
        public void setUdf3(String value) { this.udf3 = value; }
    }
}