package com.secura.dnft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "socirty_collection_types")
public class SocirtyCollectionTypes {

	@Id
	@Column(name = "collection_type")
	private String collectionType;

	@Column(name = "purpose_of_collection", columnDefinition = "TEXT")
	private String purposeOfCollection;

	@Column(name = "sac_code")
	private String sacCode;

	@Column(name = "taxable")
	private boolean taxable;

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
}
