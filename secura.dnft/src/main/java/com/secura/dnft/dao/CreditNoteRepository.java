package com.secura.dnft.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.CreditNoteEntity;
import com.secura.dnft.entity.CreditNoteEntityId;

public interface CreditNoteRepository extends JpaRepository<CreditNoteEntity, CreditNoteEntityId> {

	Optional<CreditNoteEntity> findByApartmentIdAndFlatId(String apartmentId, String flatId);
}
