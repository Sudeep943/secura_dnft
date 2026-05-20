package com.secura.dnft.bean;

import java.time.LocalDateTime;

public class WorklistAssignmentFlow {

	private String profileId;
	private LocalDateTime assignmentDateTime;
	private String status;

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public LocalDateTime getAssignmentDateTime() {
		return assignmentDateTime;
	}

	public void setAssignmentDateTime(LocalDateTime assignmentDateTime) {
		this.assignmentDateTime = assignmentDateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
