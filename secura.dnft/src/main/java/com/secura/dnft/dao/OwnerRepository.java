package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, String> {

}
