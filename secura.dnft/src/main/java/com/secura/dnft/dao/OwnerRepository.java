package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.OwnerId;

public interface OwnerRepository extends JpaRepository<Owner, OwnerId> {

	  List<Owner> findByFlatNo(String flatNo);

	  @Query("SELECT o FROM Owner o WHERE o.aprmt_id = :aprmtId AND o.flatNo = :flatNo")
	  List<Owner> findByAprmt_idAndFlatNo(@Param("aprmtId") String aprmtId, @Param("flatNo") String flatNo);
	  
	  @Query("SELECT o FROM Owner o WHERE o.ownerId = :ownerId AND o.flatNo = :flatNo AND o.aprmt_id = :aprmtId")
	  Optional<Owner> findByOwnerIdAndFlatNoAndAprmt_id(@Param("ownerId") String ownerId, @Param("flatNo") String flatNo, @Param("aprmtId") String aprmtId);
}
