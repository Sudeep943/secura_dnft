package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Flat;

public interface FlatRepository extends JpaRepository<Flat, String> {

	List<Flat> findByAprmntId(String aprmntId);

}
