package com.secura.dnft.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProfileAccountDetails {

	private String apartmentId;
	
	@JsonProperty("flatId")
	private List<String> flatIds;
	private String apartmentName;
	private String role;
	private String profileType;
	private String position;
	private String status;
	
	public String getApartmentId() {
		return apartmentId;
	}
	public void setApartmentId(String apartmentId) {
		this.apartmentId = apartmentId;
	}
	public List<String> getFlatId() {
		return flatIds;
	}
	public void setFlatId(List<String> flatId) {
		this.flatIds = flatId;
	}
	public String getApartmentName() {
		return apartmentName;
	}
	public void setApartmentName(String apartmentName) {
		this.apartmentName = apartmentName;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getProfileType() {
		return profileType;
	}
	public void setProfileType(String profileType) {
		this.profileType = profileType;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
}
