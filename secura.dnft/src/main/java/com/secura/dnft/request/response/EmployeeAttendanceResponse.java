package com.secura.dnft.request.response;

import java.util.List;

public class EmployeeAttendanceResponse {

    private boolean success;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private List<AttendanceRecordItem> records;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public List<AttendanceRecordItem> getRecords() { return records; }
    public void setRecords(List<AttendanceRecordItem> records) { this.records = records; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
