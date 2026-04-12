package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;

public interface DiscFinInterface {

	AddDiscfinResponse addDiscfin(AddDiscfinRequest request) throws Exception;

	GetDiscfinResponse getDiscfin(GetDiscfinRequest request) throws Exception;
}
