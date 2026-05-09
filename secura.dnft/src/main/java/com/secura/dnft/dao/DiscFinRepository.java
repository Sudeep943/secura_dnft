package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DiscFinId;

public interface DiscFinRepository extends JpaRepository<DiscFin, DiscFinId> {

	List<DiscFin> findByAprmtId(String aprmtId);

	List<DiscFin> findByDiscFnId(String discFnId);

	boolean existsByDiscFnId(String discFnId);

	@Transactional
	void deleteByDiscFnId(String discFnId);
}
