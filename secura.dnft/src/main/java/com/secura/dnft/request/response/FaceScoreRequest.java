package com.secura.dnft.request.response;

/**
 * Request body for the POST /attendance/face-score diagnostic endpoint.
 *
 * <p>This endpoint returns the best cosine similarity score for the submitted
 * image WITHOUT logging any attendance record. Use it from the UI or during
 * setup to verify that face recognition is working and to check whether a
 * specific photo would pass the configured match threshold.
 */
public class FaceScoreRequest {

    private String deviceId;
    private String imageBase64;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
