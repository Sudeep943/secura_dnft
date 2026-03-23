package com.secura.dnft.request.response;

import com.secura.dnft.generic.bean.Filter;

public class GetBookingRequest {
	
	private GenericHeader genericHeader;
	private Filter filter;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public Filter getFilter() {
		return filter;
	}
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	

}
