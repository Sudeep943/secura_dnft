package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.AddFlatDetailsRequest;
import com.secura.dnft.request.response.AddFlatDetailsResponse;
import com.secura.dnft.request.response.UpdateFlatDetailsRequest;
import com.secura.dnft.request.response.UpdateFlatDetailsResponse;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;

public interface FlatInterface {

	AddFlatDetailsResponse addFlatDetails(AddFlatDetailsRequest request);

	UpdateFlatDetailsResponse updateFlatDetails(UpdateFlatDetailsRequest request);

	UploadFlatDetailsResponse uploadFlatDetails(UploadFlatDetailsRequest request);
}
