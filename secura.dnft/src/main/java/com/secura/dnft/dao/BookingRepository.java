package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, String>{

	
	
	@Query("SELECT COUNT(b) FROM Booking b WHERE b.bkngHallId = :hallId AND DATE(b.bkngEvntDt) = DATE(:eventDate)")
	long countBookings(@Param("hallId") String hallId,
	                   @Param("eventDate") LocalDateTime eventDate);

	 List<Booking> findTop5ByBkngEvntDtAfterOrderByBkngEvntDtAsc(LocalDateTime currentDateTime);
	 
	 List<Booking> findTop5ByBkngStsAndBkngEvntDtAfterOrderByBkngEvntDtAsc(
		        String status,
		        LocalDateTime now
		);
}
