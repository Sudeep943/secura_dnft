package com.secura.access;

public class MeetingAndNoticeAccess {

	private boolean parentAccess;
	private boolean scheduleMeetingAccess;
	private boolean updateMeetingAccess;
	private boolean updateMOMAccess;
	private boolean createNoticeAccess;
	private boolean createEventAccess;
	private boolean updateEventAccess;
	private boolean createPollAccess;
	
	
	public boolean isParentAccess() {
		return parentAccess;
	}
	public void setParentAccess(boolean parentAccess) {
		this.parentAccess = parentAccess;
	}
	public boolean isScheduleMeetingAccess() {
		return scheduleMeetingAccess;
	}
	public void setScheduleMeetingAccess(boolean scheduleMeetingAccess) {
		this.scheduleMeetingAccess = scheduleMeetingAccess;
	}
	public boolean isUpdateMeetingAccess() {
		return updateMeetingAccess;
	}
	public void setUpdateMeetingAccess(boolean updateMeetingAccess) {
		this.updateMeetingAccess = updateMeetingAccess;
	}
	public boolean isUpdateMOMgAccess() {
		return updateMOMAccess;
	}
	public void setUpdateMOMgAccess(boolean updateMOMAccess) {
		this.updateMOMAccess = updateMOMAccess;
	}
	public boolean isCreateNoticeAccess() {
		return createNoticeAccess;
	}
	public void setCreateNoticeAccess(boolean createNoticeAccess) {
		this.createNoticeAccess = createNoticeAccess;
	}
	public boolean isCreateEventAccess() {
		return createEventAccess;
	}
	public void setCreateEventAccess(boolean createEventAccess) {
		this.createEventAccess = createEventAccess;
	}
	public boolean isUpdateEventAccess() {
		return updateEventAccess;
	}
	public void setUpdateEventAccess(boolean updateEventAccess) {
		this.updateEventAccess = updateEventAccess;
	}
	public boolean isCreatePollAccess() {
		return createPollAccess;
	}
	public void setCreatePollAccess(boolean createPollAccess) {
		this.createPollAccess = createPollAccess;
	}

	
}
