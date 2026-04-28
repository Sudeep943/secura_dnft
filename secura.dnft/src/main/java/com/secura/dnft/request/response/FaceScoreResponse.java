package com.secura.dnft.request.response;

/**
 * Response for the POST /attendance/face-score diagnostic endpoint.
 * Returns the best cosine similarity score for the submitted image without
 * logging any attendance record. Use this from the UI to verify that face
 * recognition is working and to tune the match threshold.
 */
public class FaceScoreResponse {

    /** Whether any face embedding was successfully extracted from the image. */
    private boolean faceDetected;

    /** The best cosine similarity score found across all enrolled employees (0.0 to 1.0). */
    private Double bestScore;

    /** Employee code of the closest match (null if no templates exist). */
    private String bestMatchEmployeeCode;

    /** Employee name of the closest match (null if no templates exist). */
    private String bestMatchEmployeeName;

    /** The configured match threshold for reference. */
    private Double threshold;

    /** Whether the best score exceeds the threshold (i.e. would be accepted as a match). */
    private Boolean wouldMatch;

    /** Human-readable explanation. */
    private String message;

    public boolean isFaceDetected() { return faceDetected; }
    public void setFaceDetected(boolean faceDetected) { this.faceDetected = faceDetected; }

    public Double getBestScore() { return bestScore; }
    public void setBestScore(Double bestScore) { this.bestScore = bestScore; }

    public String getBestMatchEmployeeCode() { return bestMatchEmployeeCode; }
    public void setBestMatchEmployeeCode(String bestMatchEmployeeCode) { this.bestMatchEmployeeCode = bestMatchEmployeeCode; }

    public String getBestMatchEmployeeName() { return bestMatchEmployeeName; }
    public void setBestMatchEmployeeName(String bestMatchEmployeeName) { this.bestMatchEmployeeName = bestMatchEmployeeName; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public Boolean getWouldMatch() { return wouldMatch; }
    public void setWouldMatch(Boolean wouldMatch) { this.wouldMatch = wouldMatch; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
