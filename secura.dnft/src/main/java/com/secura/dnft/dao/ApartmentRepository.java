package com.secura.dnft.dao;


import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.ApartmentMaster;


public interface ApartmentRepository extends JpaRepository<ApartmentMaster, String> {

}