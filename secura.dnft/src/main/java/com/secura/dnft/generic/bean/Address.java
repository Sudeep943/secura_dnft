package com.secura.dnft.generic.bean;

import java.util.ArrayList;
import java.util.List;

public class Address {

	
	private String addressLine1;
	private String addressLine2;
	private String addressLine3;
	private String addressLine4;
	private String landmark;
	private String city;
	private String state;
	private String postOffice;
	private String policeStation;
	private String pin;
	private String addressType;
	
	
	public String getAddressType() {
		return addressType;
	}
	public void setAddressType(String addressType) {
		this.addressType = addressType;
	}
	public String getAddressLine1() {
		return addressLine1;
	}
	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}
	public String getAddressLine2() {
		return addressLine2;
	}
	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}
	public String getAddressLine3() {
		return addressLine3;
	}
	public void setAddressLine3(String addressLine3) {
		this.addressLine3 = addressLine3;
	}
	public String getAddressLine4() {
		return addressLine4;
	}
	public void setAddressLine4(String addressLine4) {
		this.addressLine4 = addressLine4;
	}
	public String getLandmark() {
		return landmark;
	}
	public void setLandmark(String landmark) {
		this.landmark = landmark;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getPostOffice() {
		return postOffice;
	}
	public void setPostOffice(String postOffice) {
		this.postOffice = postOffice;
	}
	public String getPoliceStation() {
		return policeStation;
	}
	public void setPoliceStation(String policeStation) {
		this.policeStation = policeStation;
	}
	public String getPin() {
		return pin;
	}
	public void setPin(String pin) {
		this.pin = pin;
	}
	
	@Override
	public String toString() {
		List<String> parts = new ArrayList<>();
		addIfPresent(parts, addressLine1);
		addIfPresent(parts, addressLine2);
		addIfPresent(parts, addressLine3);
		addIfPresent(parts, addressLine4);
		addIfPresent(parts, landmark);
		addIfPresent(parts, city);
		addIfPresent(parts, state);
		addIfPresent(parts, postOffice);
		addIfPresent(parts, policeStation);
		addIfPresent(parts, pin);
		addIfPresent(parts, addressType);
		return String.join(" ,", parts);
	}

	private void addIfPresent(List<String> parts, String value) {
		if (value != null && !value.isBlank()) {
			parts.add(value);
		}
	}
	
	
}
