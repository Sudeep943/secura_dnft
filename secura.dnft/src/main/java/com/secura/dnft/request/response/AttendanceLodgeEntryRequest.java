package com.secura.dnft.request.response;

public class AttendanceLodgeEntryRequest {

    private String deviceId;
    private String imageBase64;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
