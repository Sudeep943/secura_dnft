package com.secura.dnft.request.response;

import com.secura.dnft.entity.DiscFin;

public class UpdateDiscfinRequest {

	private GenericHeader genericHeader;
	private String discFinId;
	private DiscFin discfinEntity;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getDiscFinId() {
		return discFinId;
	}

	public void setDiscFinId(String discFinId) {
		this.discFinId = discFinId;
	}

	public DiscFin getDiscfinEntity() {
		return discfinEntity;
	}

	public void setDiscfinEntity(DiscFin discfinEntity) {
		this.discfinEntity = discfinEntity;
	}
}
