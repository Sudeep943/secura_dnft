package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.DeleteDiscfinRequest;
import com.secura.dnft.request.response.DeleteDiscfinResponse;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;
import com.secura.dnft.service.DiscFinServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/discfin")
public class DiscFinController {

	@Autowired
	private DiscFinServices discFinServices;

	@PostMapping("/addDiscfin")
	public AddDiscfinResponse addDiscfin(@RequestBody AddDiscfinRequest request) {
		AddDiscfinResponse response = new AddDiscfinResponse();
		try {
			response = discFinServices.addDiscfin(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/getDiscfin")
	public GetDiscfinResponse getDiscfin(@RequestBody GetDiscfinRequest request) {
		GetDiscfinResponse response = new GetDiscfinResponse();
		try {
			response = discFinServices.getDiscfin(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/deleteDiscfin")
	public DeleteDiscfinResponse deleteDiscfin(@RequestBody DeleteDiscfinRequest request) {
		DeleteDiscfinResponse response = new DeleteDiscfinResponse();
		try {
			response = discFinServices.deleteDiscfin(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
