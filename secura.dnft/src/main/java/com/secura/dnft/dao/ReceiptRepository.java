package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.secura.dnft.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {

	@Query("SELECT r.receiptId FROM Receipt r WHERE r.receiptId LIKE CONCAT(:prefix, '%') "
			+ "AND LENGTH(r.receiptId) = :expectedLength ORDER BY r.receiptId DESC")
	List<String> findLatestReceiptIdsByPrefix(@Param("prefix") String prefix, @Param("expectedLength") int expectedLength, Pageable pageable);

}
