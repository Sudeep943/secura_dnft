package com.secura.dnft.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.LoginRequest;
import com.secura.dnft.request.response.LoginResponse;
import com.secura.dnft.security.JwtUtil;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    JwtUtil jwtUtil;

    @PostMapping("/login")
    @CrossOrigin(origins = "*")
    public LoginResponse login(@RequestBody LoginRequest request) {
    	LoginResponse loginResponse= new LoginResponse();
    	GenericHeader genericHeader = new GenericHeader();
    	genericHeader.setUserId("PRFL260300174587");
    	genericHeader.setApartmentId("DNAPR01");
    	genericHeader.setRole("RESIDENT");
    	genericHeader.setFlatNo("1236");
    	
        if(request.getUsername().equals("admin")
                && request.getPassword().equals("password")) {
        	loginResponse.setToken( jwtUtil.generateToken(request.getUsername()));
        	genericHeader.setAccess("ADMIN");
        	loginResponse.setHeader(genericHeader);
            return loginResponse;
        }
        else if(request.getUsername().equals("Resident")
                && request.getPassword().equals("password")){
        	loginResponse.setToken( jwtUtil.generateToken(request.getUsername()));
        	genericHeader.setAccess("RESIDENT");
        	loginResponse.setHeader(genericHeader);
        	 return loginResponse;
        }

        throw new RuntimeException("Invalid credentials");
    }
}