package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GetSocirtyCollectionTypesResponse;
import com.secura.dnft.service.SocirtyCollectionTypesServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/socirtyCollectionTypes")
public class SocirtyCollectionTypesController {

	@Autowired
	private SocirtyCollectionTypesServices socirtyCollectionTypesServices;

	@GetMapping("/getAll")
	public GetSocirtyCollectionTypesResponse getAllSocirtyCollectionTypes() {
		GetSocirtyCollectionTypesResponse response = new GetSocirtyCollectionTypesResponse();
		try {
			return socirtyCollectionTypesServices.getAllSocirtyCollectionTypes();
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
