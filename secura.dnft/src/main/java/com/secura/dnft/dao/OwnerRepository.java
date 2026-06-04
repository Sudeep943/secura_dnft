package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.OwnerId;

public interface OwnerRepository extends JpaRepository<Owner, OwnerId> {

	  List<Owner> findByFlatNo(String flatNo);

	  List<Owner> findByAprmt_idAndFlatNo(String aprmt_id, String flatNo);
	  
	  Optional<Owner> findByOwnerIdAndFlatNoAndAprmt_id(String ownerId, String flatNo, String aprmt_id);
}
