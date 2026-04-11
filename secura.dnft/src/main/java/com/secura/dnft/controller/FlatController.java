package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.AddFlatDetailsRequest;
import com.secura.dnft.request.response.AddFlatDetailsResponse;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.UpdateFlatDetailsRequest;
import com.secura.dnft.request.response.UpdateFlatDetailsResponse;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;
import com.secura.dnft.service.FlatServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/flat")
public class FlatController {

	@Autowired
	private FlatServices flatServices;

	@PostMapping("/addFlatDetails")
	@CrossOrigin(origins = "*")
	public AddFlatDetailsResponse addFlatDetails(@RequestBody AddFlatDetailsRequest request) {
		AddFlatDetailsResponse response = new AddFlatDetailsResponse();
		try {
			response = flatServices.addFlatDetails(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/updateFlatDetails")
	@CrossOrigin(origins = "*")
	public UpdateFlatDetailsResponse updateFlatDetails(@RequestBody UpdateFlatDetailsRequest request) {
		UpdateFlatDetailsResponse response = new UpdateFlatDetailsResponse();
		try {
			response = flatServices.updateFlatDetails(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/uploadFlatDetails")
	@CrossOrigin(origins = "*")
	public UploadFlatDetailsResponse uploadFlatDetails(@RequestBody UploadFlatDetailsRequest request) {
		UploadFlatDetailsResponse response = new UploadFlatDetailsResponse();
		try {
			response = flatServices.uploadFlatDetails(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@GetMapping("/getSampleExcellToUploadData")
	@CrossOrigin(origins = "*")
	public GetSampleExcellToUploadDataResponse getSampleExcellToUploadData() {
		GetSampleExcellToUploadDataResponse response = new GetSampleExcellToUploadDataResponse();
		try {
			response = flatServices.getSampleExcellToUploadData();
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
