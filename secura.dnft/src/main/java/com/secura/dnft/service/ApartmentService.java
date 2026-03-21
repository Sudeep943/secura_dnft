package com.secura.dnft.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.entity.ApartmentMaster;

@Service
public class ApartmentService {

	
	
	 @Autowired
	    private ApartmentRepository repository;

	    public List<ApartmentMaster> getAllApartments() {
	        return repository.findAll();
	    }

}
