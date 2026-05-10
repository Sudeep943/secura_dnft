package com.secura.dnft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "society_collection_types")
public class SocietyCollectionTypes {

	@Id
	@Column(name = "collection_type")
	private String collectionType;

	@Column(name = "purpose_of_collection", columnDefinition = "TEXT")
	private String purposeOfCollection;

	@Column(name = "sac_code")
	private String sacCode;

	@Column(name = "taxable")
	private boolean taxable;
	
	@Column(name = "type_constant")
	private boolean typeConstant;

	public String getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public String getPurposeOfCollection() {
		return purposeOfCollection;
	}

	public void setPurposeOfCollection(String purposeOfCollection) {
		this.purposeOfCollection = purposeOfCollection;
	}

	public String getSacCode() {
		return sacCode;
	}

	public void setSacCode(String sacCode) {
		this.sacCode = sacCode;
	}

	public boolean isTaxable() {
		return taxable;
	}

	public void setTaxable(boolean taxable) {
		this.taxable = taxable;
	}

	public boolean isTypeConstant() {
		return typeConstant;
	}

	public void setTypeConstant(boolean typeConstant) {
		this.typeConstant = typeConstant;
	}
	
}
