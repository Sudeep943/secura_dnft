package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.DeTagDiscFinFromPaymentResponse;
import com.secura.dnft.request.response.DeleteDiscfinRequest;
import com.secura.dnft.request.response.DeleteDiscfinResponse;
import com.secura.dnft.request.response.DetagDiscFinFromPaymentRequest;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;
import com.secura.dnft.request.response.TagDiscFinFromPaymentRequest;
import com.secura.dnft.request.response.TagDiscFinFromPaymentResponse;
import com.secura.dnft.request.response.UpdateDiscfinRequest;
import com.secura.dnft.request.response.UpdateDiscfinResponse;

public interface DiscFinInterface {

	AddDiscfinResponse addDiscfin(AddDiscfinRequest request) throws Exception;

	GetDiscfinResponse getDiscfin(GetDiscfinRequest request) throws Exception;

	DeleteDiscfinResponse deleteDiscfin(DeleteDiscfinRequest request) throws Exception;

	UpdateDiscfinResponse updateDiscfin(UpdateDiscfinRequest request) throws Exception;

	TagDiscFinFromPaymentResponse tagDiscFinFromPayment(TagDiscFinFromPaymentRequest request) throws Exception;
	
	DeTagDiscFinFromPaymentResponse deTagDiscFinFromPayment(DetagDiscFinFromPaymentRequest request) throws Exception;
}
