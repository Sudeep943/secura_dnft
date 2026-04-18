package com.secura.dnft.bean;

import java.sql.Date;
import java.util.List;

public class WorkListAssignment {

	private Date assignmentDate;
	private Date completedDate;
	private List<String> assignedPersonList;
	private String currentStatus;
	private String assignedBy;

	public Date getAssignmentDate() {
		return assignmentDate;
	}

	public void setAssignmentDate(Date assignmentDate) {
		this.assignmentDate = assignmentDate;
	}

	public Date getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(Date completedDate) {
		this.completedDate = completedDate;
	}

	public List<String> getAssignedPersonList() {
		return assignedPersonList;
	}

	public void setAssignedPersonList(List<String> assignedPersonList) {
		this.assignedPersonList = assignedPersonList;
	}

	public String getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	public String getAssignedBy() {
		return assignedBy;
	}

	public void setAssignedBy(String assignedBy) {
		this.assignedBy = assignedBy;
	}
}
