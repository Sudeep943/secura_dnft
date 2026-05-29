package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.TransactionId;

public interface TransactionRepository extends JpaRepository<Transaction, TransactionId> {

	List<Transaction> findByPymntIdAndFlatIdAndTrnsStatus(String pymntId, String flatId, String trnsStatus);

	List<Transaction> findByAprmntIdAndPymntIdAndTrnsStatus(String aprmntId, String pymntId, String trnsStatus);

	List<Transaction> findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus(
			String aprmntId, String pymntId, String flatId, String trnsStatus);

	List<Transaction> findByAprmntIdAndPymntIdInAndTrnsStatus(String aprmntId, List<String> pymntIds, String trnsStatus);

	List<Transaction> findByAprmntId(String aprmntId);

	List<Transaction> findByAprmntIdAndTrnsTypeAndTrnsDateBetween(
			String aprmntId, String trnsType, LocalDateTime from, LocalDateTime to);

	List<Transaction> findByAprmntIdAndTrnsTypeAndTrnsStatusAndTrnsDateBetween(
			String aprmntId, String trnsType, String trnsStatus, LocalDateTime from, LocalDateTime to);

	List<Transaction> findByAprmntIdAndTrnscId(String aprmntId, String trnscId);

	long countByAprmntIdAndPymntId(String aprmntId, String pymntId);
}
