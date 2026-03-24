package com.secura.dnft.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.DashBordDataResponce;
import com.secura.dnft.request.response.GenericHeader;


@Service
public class GenericService {

	@Autowired
	BookingRepository bookingRepository;
	
	@Autowired
	WorklistRepository worklistRepository;
	
	public DashBordDataResponce getDashBoardData(GenericHeader header) {
		DashBordDataResponce bordDataResponce= new DashBordDataResponce();
		List<Booking> upcomingBookings=getUpcomingHallBooking();
		long pendingCount=getPendingWorkListCount();
		bordDataResponce.setHeader(header);
		bordDataResponce.setPendingWorklistCount(pendingCount);
		bordDataResponce.setUpcomingBookings(upcomingBookings);
		return bordDataResponce;
	}
	
	public List<Booking> getUpcomingHallBooking() {
		List<Booking> upcomingBookings = bookingRepository
		        .findTop5ByBkngStsAndBkngEvntDtAfterOrderByBkngEvntDtAsc(SecuraConstants.BOOKING_CONST_STATUS_APPROVED,LocalDateTime.now());
		return upcomingBookings;
	}
	
	public long getPendingWorkListCount() {
		long pendingCount = worklistRepository.countByStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		return pendingCount;
	}
	
	public Worklist createWorklist(String worklistType,String createdBy) {
		Worklist worklist = new Worklist();
		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		worklist.setWorklistsType(worklistType);
		worklist.setWorklistTaskId(createWorklistId(worklistType,createdBy));
		worklist.setCreatUsrId(createdBy);
		worklist.setCreatTs( LocalDateTime.now());
		worklistRepository.save(worklist);
		return worklist;
	}
	
	public void canelWorklist(String worklistId) {
		Optional<Worklist> worklist=worklistRepository.findById(worklistId);
		if(worklist.isPresent()) {
			worklist.get().setStatus(SecuraConstants.WORKLIST_STATUS_CANCELLED);
			worklistRepository.save(worklist.get());
		}
	}
	
	public void getLatestPaymentsCredit() {}

	public String createWorklistId(String worklistType,String createdBy) {
		StringBuffer worklistId= new StringBuffer();
		worklistId.append(worklistType +"_");
		worklistId.append(createdBy +"_");
		Random random = new Random();
		worklistId.append( 1000 + random.nextInt(9000));
		return worklistId.toString();
	}
	
}
