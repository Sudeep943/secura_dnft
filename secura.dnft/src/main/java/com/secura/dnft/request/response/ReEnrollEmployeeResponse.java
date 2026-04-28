package com.secura.dnft.request.response;

public class ReEnrollEmployeeResponse {

    private boolean success;
    private Long employeeId;
    private String employeeCode;
    private int templatesStored;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public int getTemplatesStored() { return templatesStored; }
    public void setTemplatesStored(int templatesStored) { this.templatesStored = templatesStored; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
