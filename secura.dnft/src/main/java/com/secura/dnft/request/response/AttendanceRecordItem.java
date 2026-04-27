package com.secura.dnft.request.response;

import java.time.LocalDateTime;

public class AttendanceRecordItem {

    private Long logId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String deviceId;
    private Double matchScoreEntry;
    private Double matchScoreExit;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Double getMatchScoreEntry() { return matchScoreEntry; }
    public void setMatchScoreEntry(Double matchScoreEntry) { this.matchScoreEntry = matchScoreEntry; }

    public Double getMatchScoreExit() { return matchScoreExit; }
    public void setMatchScoreExit(Double matchScoreExit) { this.matchScoreExit = matchScoreExit; }
}
