package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.BookingRequest;
import com.secura.dnft.request.response.BookingResponse;
import com.secura.dnft.request.response.UpdateBookingRequest;
import com.secura.dnft.request.response.UpdateBookingResponse;
import com.secura.dnft.request.response.CheckHallAvailablityRequest;
import com.secura.dnft.request.response.CheckHallAvailablityResponse;
import com.secura.dnft.request.response.GetBookingRequest;
import com.secura.dnft.request.response.GetBookingResponse;
import com.secura.dnft.request.response.GetHallsReponse;
import com.secura.dnft.request.response.GetUpcomigBookingRequest;
import com.secura.dnft.request.response.GetUpcomigBookingResponse;
import com.secura.dnft.service.BookingService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/booking")
public class BookingController {
	
	
	 @Autowired
	 private BookingService bookingService;
	 
    @PostMapping("/bookHall")
    @CrossOrigin(origins = "*")
    public BookingResponse bookHall(@RequestBody BookingRequest request) {
    	BookingResponse bookingResponse = new BookingResponse();
    	bookingResponse=bookingService.createBooking(request);
    	return bookingResponse;
            }
    
    @PostMapping("/getBookings")
    @CrossOrigin(origins = "*")
    public GetBookingResponse getYourBooking(@RequestBody GetBookingRequest request) {
    	GetBookingResponse bookingResponse = new GetBookingResponse();
    	bookingResponse=bookingService.getAllBooking(request);
    	return bookingResponse;
            }
    
    @PostMapping("/getBooking")
    @CrossOrigin(origins = "*")
    public GetBookingResponse getBooking(@RequestBody GetBookingRequest request) {
    	GetBookingResponse bookingResponse = new GetBookingResponse();
    	bookingResponse=bookingService.getBooking(request);
    	return bookingResponse;
            }

    
    @GetMapping("/getAllHalls/{apartmentId}")
    @CrossOrigin(origins = "*")    
    public GetHallsReponse getAllHalls(@PathVariable("apartmentId") String apartmentId) {
    	GetHallsReponse hetHallsReponse = new GetHallsReponse();
    	hetHallsReponse=bookingService.getAllHals(apartmentId);
    	return hetHallsReponse;
            }
    
    
    @PostMapping("/checkHallAvailability")
    @CrossOrigin(origins = "*")
    public CheckHallAvailablityResponse checkHallAvailability(@RequestBody CheckHallAvailablityRequest checkHallAvailablityRequest) {
    	CheckHallAvailablityResponse checkHallAvailablityResponse=bookingService.checkAvailabilityOfHall(checkHallAvailablityRequest);
    	return checkHallAvailablityResponse;
            }
    
    @PostMapping("/updateBooking")
    @CrossOrigin(origins = "*")
    public UpdateBookingResponse updateBooking(@RequestBody UpdateBookingRequest request) {
    	UpdateBookingResponse response= bookingService.updateBooking(request);
    	return response;
            }
    
    @PostMapping("/bookAppliance")
    public String bookAppliance(@RequestBody BookingRequest request) {

    	return null;
            }
    
    @PostMapping("/checkApplianceAvailability")
    public String checkApplianceAvailability(@RequestBody BookingRequest request) {

    	return null;
            }
    
    @PostMapping("/updateApplianceBooking")
    public String updateApplianceBooking(@RequestBody BookingRequest request) {

    	return null;
            }

    @PostMapping("/getUpcomingHallBookings")
    public GetUpcomigBookingResponse getUpcomingHallBooking(@RequestBody GetUpcomigBookingRequest request) {
    	GetUpcomigBookingResponse bookingResponse=bookingService.getUpcomingHallBooking(request);
    	return bookingResponse;
            }
}
