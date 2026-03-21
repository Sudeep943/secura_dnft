package com.secura.dnft.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.secura.dnft.request.response.LoginRequest;
import com.secura.dnft.security.JwtUtil;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    JwtUtil jwtUtil;

    @PostMapping("/login")
    @CrossOrigin(origins = "*")
    public String login(@RequestBody LoginRequest request) {

        if(request.getUsername().equals("admin")
                && request.getPassword().equals("password")) {

            return jwtUtil.generateToken(request.getUsername());
        }

        throw new RuntimeException("Invalid credentials");
    }
}