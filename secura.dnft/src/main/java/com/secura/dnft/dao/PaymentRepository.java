package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.PaymentEntityId;

public interface PaymentRepository extends JpaRepository<PaymentEntity, PaymentEntityId> {

	List<PaymentEntity> findByAprmtId(String aprmtId);

	List<PaymentEntity> findByPaymentId(String paymentId);

	List<PaymentEntity> findByPaymentIdAndAprmtId(String paymentId, String aprmtId);

	Optional<PaymentEntity> findFirstByPaymentId(String paymentId);

	Optional<PaymentEntity> findFirstByPaymentIdAndAprmtId(String paymentId, String aprmtId);

	long countByAprmtIdAndCauseIdIgnoreCase(String aprmtId, String causeId);

	List<PaymentEntity> findByEmailSentflag(String emailSentflag);
	
	 @Query(value = """
		        SELECT *
		        FROM secura_payments
		        WHERE payment_id IN (:paymentIds)
		          AND aprmt_id = :aprmntId
		        """, nativeQuery = true)
		    List<PaymentEntity> findByPaymentIdsAndAprmntId(
		            @Param("paymentIds") List<String> paymentIds,
		            @Param("aprmntId") String aprmntId
		    );
}
