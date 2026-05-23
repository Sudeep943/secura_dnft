package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.entity.BankEntityId;

public interface BankEntityRepository extends JpaRepository<BankEntity, BankEntityId> {

	List<BankEntity> findByAprmntId(String aprmntId);

	Optional<BankEntity> findByAprmntIdAndBankDetailsID(String aprmntId, String bankDetailsID);
}
