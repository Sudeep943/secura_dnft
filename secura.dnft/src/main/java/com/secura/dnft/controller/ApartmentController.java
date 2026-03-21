package com.secura.dnft.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.entity.ApartmentMaster;
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
}