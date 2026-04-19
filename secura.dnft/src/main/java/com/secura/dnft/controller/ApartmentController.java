package com.secura.dnft.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GetApartmentDetailsRequest;
import com.secura.dnft.request.response.GetApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateApartmentDetailsRequest;
import com.secura.dnft.request.response.UpdateApartmentDetailsResponse;
import com.secura.dnft.service.ApartmentService;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    @Autowired
    private ApartmentService service;

    @GetMapping
    @CrossOrigin(origins = "*")
    public List<ApartmentMaster> getApartments() {
    	 List<ApartmentMaster> list=service.getAllApartments();
    	 System.out.println("DATA: " + list);
        return list;
    }

    @PostMapping({"/updateApartmentDetails", "/updateApatrmentdetails"})
    @CrossOrigin(origins = "*")
    public UpdateApartmentDetailsResponse updateApartmentDetails(@RequestBody UpdateApartmentDetailsRequest request) {
    	UpdateApartmentDetailsResponse response = new UpdateApartmentDetailsResponse();
    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
    	try {
    		return service.updateApartmentDetails(request);
    	} catch (Exception e) {
    		response.setMessage(ErrorMessage.ERR_MESSAGE_33);
    		response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
    	}
    	return response;
    }

    @PostMapping({"/getApartmentDetails", "/getApatrmentdetails"})
    @CrossOrigin(origins = "*")
    public GetApartmentDetailsResponse getApartmentDetails(@RequestBody GetApartmentDetailsRequest request) {
    	GetApartmentDetailsResponse response = new GetApartmentDetailsResponse();
    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
    	try {
    		return service.getApartmentDetails(request);
    	} catch (Exception e) {
    		response.setMessage(ErrorMessage.ERR_MESSAGE_33);
    		response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
    	}
    	return response;
    }
}
