package com.secura.dnft.request.response;

import java.time.LocalDateTime;

public class AttendanceMarkExitResponse {

    private boolean matched;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDateTime exitTime;
    private Double matchScore;
    private String message;

    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
