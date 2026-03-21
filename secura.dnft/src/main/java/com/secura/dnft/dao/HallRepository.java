package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Halls;

public interface HallRepository  extends JpaRepository<Halls, String>{

}
