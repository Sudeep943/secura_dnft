package com.secura.dnft.request.response;

public class AddedCharges {

	private String chargeName;
	private String chargeType;
	private String value;
	private String finalChargeValue;

	public String getChargeName() {
		return chargeName;
	}

	public void setChargeName(String chargeName) {
		this.chargeName = chargeName;
	}

	public String getChargeType() {
		return chargeType;
	}

	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFinalChargeValue() {
		return finalChargeValue;
	}

	public void setFinalChargeValue(String finalChargeValue) {
		this.finalChargeValue = finalChargeValue;
	}
}
