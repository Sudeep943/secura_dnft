package com.secura.dnft.request.response;

/**
 * Holds the OTP verification details required for credit-note payment authorization.
 */
public class OtpDetails {

    private String otp;
    private String otpId;
    private String sessionId;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getOtpId() {
        return otpId;
    }

    public void setOtpId(String otpId) {
        this.otpId = otpId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
