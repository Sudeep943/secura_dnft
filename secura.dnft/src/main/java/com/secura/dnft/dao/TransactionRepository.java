package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

	@Query("SELECT t FROM Transaction t WHERE t.aprmntId = :aprmntId AND t.trnsDate >= :fromDate AND t.trnsDate <= :toDate")
	List<Transaction> findByAprmntIdAndTrnsDateBetween(@Param("aprmntId") String aprmntId,
			@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
