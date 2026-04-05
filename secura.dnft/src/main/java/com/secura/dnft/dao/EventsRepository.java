package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.EventsEntity;

public interface EventsRepository extends JpaRepository<EventsEntity, String> {

	
}
