package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, String>{

	@Query(value = "SELECT * FROM secura_profl p WHERE " +
	        "(" +
	        "LOWER((p.prfl_name::jsonb ->> 'firstName')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
	        "LOWER((p.prfl_name::jsonb ->> 'middleName')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
	        "LOWER((p.prfl_name::jsonb ->> 'lastName')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +

	        "EXISTS (" +
	        "   SELECT 1 FROM jsonb_array_elements(p.prfl_acount_details::jsonb) elem " +
	        "   WHERE elem ->> 'apartmentId' = :apartmentId " +
	        "   AND (" +
	        "       EXISTS (" +
	        "           SELECT 1 FROM jsonb_array_elements_text(elem -> 'flatId') f " +
	        "           WHERE f LIKE CONCAT('%', :search, '%')" +
	        "       )" +
	        "   )" +
	        ") OR " +

	        "LOWER(p.prfl_email_adrss) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
	        "p.prfl_phone_no LIKE CONCAT('%', :search, '%')" +
	        ") " +

	        "AND EXISTS (" +
	        "   SELECT 1 FROM jsonb_array_elements(p.prfl_acount_details::jsonb) elem " +
	        "   WHERE elem ->> 'apartmentId' = :apartmentId" +
	        ")",
	        nativeQuery = true)
	List<Profile> searchProfiles(@Param("search") String search,
	                             @Param("apartmentId") String apartmentId);
}
