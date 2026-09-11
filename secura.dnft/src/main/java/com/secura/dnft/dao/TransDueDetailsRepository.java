package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.TransDueDetailsEntity;
import com.secura.dnft.entity.TransDueDetailsEntityId;

public interface TransDueDetailsRepository extends JpaRepository<TransDueDetailsEntity, TransDueDetailsEntityId> {

	List<TransDueDetailsEntity> findByPaymentIdAndAprmntId(String paymentId, String aprmntId);

	List<TransDueDetailsEntity> findByPaymentIdInAndAprmntId(List<String> paymentIds, String aprmntId);

	@Query("SELECT DISTINCT t.paymentId FROM TransDueDetailsEntity t WHERE t.paymentName = :paymentName AND t.aprmntId = :aprmntId")
	List<String> findDistinctPaymentIdsByPaymentNameAndAprmntId(@Param("paymentName") String paymentName,
			@Param("aprmntId") String aprmntId);
	
	 boolean existsByTransactionId(String transactionId);

}
