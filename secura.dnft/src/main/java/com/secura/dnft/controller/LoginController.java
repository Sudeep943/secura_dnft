package com.secura.dnft.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.LoginRequest;
import com.secura.dnft.request.response.LoginResponse;
import com.secura.dnft.request.response.UpdatePasswordRequest;
import com.secura.dnft.request.response.UpdatePasswordResponse;
import com.secura.dnft.security.JwtUtil;
import com.secura.dnft.service.LoginService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    JwtUtil jwtUtil;
    
    @Autowired
    LoginService loginService;

    @PostMapping("/login")
    @CrossOrigin(origins = "*")
    public LoginResponse login(@RequestBody LoginRequest request) {
    	LoginResponse loginResponse= loginService.login(request);
    	return loginResponse;
//    	GenericHeader genericHeader = new GenericHeader();
//    	genericHeader.setUserId("PRFL260300174587");
//    	genericHeader.setApartmentId("APT001");
//    	genericHeader.setRole("RESIDENT");
//    	genericHeader.setFlatNo("1236");
//    	
//        if(request.getUsername().equals("admin")
//                && request.getPassword().equals("password")) {
//        	loginResponse.setToken( jwtUtil.generateToken(request.getUsername()));
//        	genericHeader.setAccess("ADMIN");
//        	loginResponse.setHeader(genericHeader);
//            return loginResponse;
//        }
//        else if(request.getUsername().equals("Resident")
//                && request.getPassword().equals("password")){
//        	loginResponse.setToken( jwtUtil.generateToken(request.getUsername()));
//        	genericHeader.setAccess("RESIDENT");
//        	loginResponse.setHeader(genericHeader);
//        	 return loginResponse;
//        }
//
//        throw new RuntimeException("Invalid credentials");
    }
    

    @PostMapping("/updatePassword")
    @CrossOrigin(origins = "*")
    public UpdatePasswordResponse login(@RequestBody UpdatePasswordRequest request) {
    	UpdatePasswordResponse updatePasswordResponse= loginService.updatePassword(request);
    	return updatePasswordResponse;
    	}
}