package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.BookingRequest;
import com.secura.dnft.request.response.BookingResponse;
import com.secura.dnft.request.response.CancelBookingRequest;
import com.secura.dnft.request.response.CancelBookingResponse;
import com.secura.dnft.request.response.CheckHallAvailablityRequest;
import com.secura.dnft.request.response.CheckHallAvailablityResponse;
import com.secura.dnft.request.response.GetBookingRequest;
import com.secura.dnft.request.response.GetBookingResponse;
import com.secura.dnft.request.response.GetHallsReponse;
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

    
    @GetMapping("/getAllHalls")
    @CrossOrigin(origins = "*")    
    public GetHallsReponse getAllHalls() {
    	GetHallsReponse hetHallsReponse = new GetHallsReponse();
    	hetHallsReponse=bookingService.getAllHals();
    	return hetHallsReponse;
            }
    
    
    @PostMapping("/checkHallAvailability")
    @CrossOrigin(origins = "*")
    public CheckHallAvailablityResponse checkHallAvailability(@RequestBody CheckHallAvailablityRequest checkHallAvailablityRequest) {
    	CheckHallAvailablityResponse checkHallAvailablityResponse=bookingService.checkAvailabilityOfHall(checkHallAvailablityRequest);
    	return checkHallAvailablityResponse;
            }
    
    @PostMapping("/cancelBooking")
    @CrossOrigin(origins = "*")
    public CancelBookingResponse cancelkBooking(@RequestBody CancelBookingRequest request) {
    	CancelBookingResponse response= bookingService.cancelBooking(request);
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

}
