package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Worklist;

public interface WorklistRepository extends JpaRepository<Worklist, String>{

	
	long countByStatus(String status);
}
