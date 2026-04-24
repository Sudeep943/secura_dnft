package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

	List<Transaction> findByAprmntIdAndTrnsTypeAndTrnsDateBetween(
			String aprmntId, String trnsType, LocalDateTime from, LocalDateTime to);

	List<Transaction> findByAprmntIdAndTrnsTypeAndTrnsStatusAndTrnsDateBetween(
			String aprmntId, String trnsType, String trnsStatus, LocalDateTime from, LocalDateTime to);
}
