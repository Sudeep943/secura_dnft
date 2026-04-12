package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.DiscFin;

public interface DiscFinRepository extends JpaRepository<DiscFin, String> {

	List<DiscFin> findByAprmtId(String aprmtId);
}
