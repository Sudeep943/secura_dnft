package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.AddFlatDetailsRequest;
import com.secura.dnft.request.response.AddFlatDetailsResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetDueAmountForPerHeadCalculationRequest;
import com.secura.dnft.request.response.GetDueAmountForPerHeadCalculationResponse;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.UpdateFlatDetailsRequest;
import com.secura.dnft.request.response.UpdateFlatDetailsResponse;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;

public interface FlatInterface {

	AddFlatDetailsResponse addFlatDetails(AddFlatDetailsRequest request);

	UpdateFlatDetailsResponse updateFlatDetails(UpdateFlatDetailsRequest request);

	UploadFlatDetailsResponse uploadFlatDetails(UploadFlatDetailsRequest request);

	GetSampleExcellToUploadDataResponse getSampleExcellToUploadData();

	GetAllFlatsResponse getAllFlats(GetAllFlatsRequest request);

	GetDueAmountForFlatResponse getDueAmountForFlat(GetDueAmountForFlatRequest request);

	GetDueAmountForPerHeadCalculationResponse getDueAmountForPerHeadCalculation(
			GetDueAmountForPerHeadCalculationRequest request);
}
