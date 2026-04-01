package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, String> {

	  List<Owner> findByFlatNo(String flatNo);
}
