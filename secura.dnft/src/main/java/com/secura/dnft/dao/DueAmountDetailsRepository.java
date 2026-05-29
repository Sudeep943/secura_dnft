package com.secura.dnft.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.DueAmountDetailsEntityId;

public interface DueAmountDetailsRepository extends JpaRepository<DueAmountDetailsEntity, DueAmountDetailsEntityId> {

	List<DueAmountDetailsEntity> findByDueId(String dueId);

	List<DueAmountDetailsEntity> findByDueIdIn(List<String> dueIds);

	List<DueAmountDetailsEntity> findByPaymentId(String paymentId);

	List<DueAmountDetailsEntity> findByPaymentIdIn(List<String> paymentIds);

	Optional<DueAmountDetailsEntity> findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(
			String aprmntId, String dueId, String collectionCycle, String flatArea, LocalDate dueDate);
}
