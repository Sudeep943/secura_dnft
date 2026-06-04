package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.FlatId;

import java.util.Optional;

public interface FlatRepository extends JpaRepository<Flat, FlatId> {

	List<Flat> findByAprmntId(String aprmntId);
	Optional<Flat> findByAprmntIdAndFlatNo(String aprmntId, String flatNo);

}
