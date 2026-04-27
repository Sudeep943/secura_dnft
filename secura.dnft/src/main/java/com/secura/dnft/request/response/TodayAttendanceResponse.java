package com.secura.dnft.request.response;

import java.util.List;

public class TodayAttendanceResponse {

    private boolean success;
    private String date;
    private int totalPresent;
    private List<AttendanceRecordItem> records;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getTotalPresent() { return totalPresent; }
    public void setTotalPresent(int totalPresent) { this.totalPresent = totalPresent; }

    public List<AttendanceRecordItem> getRecords() { return records; }
    public void setRecords(List<AttendanceRecordItem> records) { this.records = records; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
