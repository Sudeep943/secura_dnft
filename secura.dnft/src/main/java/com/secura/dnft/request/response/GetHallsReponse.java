package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Halls;

public class GetHallsReponse {

	private GenericHeader genericHeader;
	private List<Halls> halls;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public List<Halls> getHalls() {
		return halls;
	}
	public void setHalls(List<Halls> halls) {
		this.halls = halls;
	}
	
	
}
