package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.DashBordDataResponce;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.service.GenericService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/generic")
public class GenericController {
	
	
	@Autowired
	 private GenericService genericService;
	
	    @PostMapping("/getDahboardData")
	    @CrossOrigin(origins = "*")
	    public DashBordDataResponce dashBordData(@RequestBody GenericHeader header) {
		  DashBordDataResponce dashBordDataResponce = new DashBordDataResponce();
		  dashBordDataResponce=genericService.getDashBoardData(header);
	    	return dashBordDataResponce;
	            }

}
