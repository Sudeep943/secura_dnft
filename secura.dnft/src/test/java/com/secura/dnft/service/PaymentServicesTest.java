package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;

class PaymentServicesTest {

    private final PaymentServices paymentServices = new PaymentServices();

    @Test
    void getDuePaymentAmountDetails_shouldReturnPostYearlyDueDateAsEndPlusOneDayAndGstAmounts() {
        DuePaymentAmountDetailsRequest request = new DuePaymentAmountDetailsRequest();
        request.setPaymentAmount("15000");
        request.setGst("10");
        request.setCollectionStartDate(LocalDate.parse("2026-03-01"));
        request.setCollectionEndDate(LocalDate.parse("2027-02-28"));
        request.setPaymentCollectionCycle("yearly");
        request.setPaymentCollectionMode("POST");
        request.setPaymentCapita("PER_FLAT");
        request.setTodayDate(LocalDate.parse("2026-10-05"));

        DuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

        assertEquals(LocalDate.parse("2027-03-01"), response.getDueDate());
        assertEquals("0", response.getAmountExcludingGst());
        assertEquals("0", response.getGstAmount());
        assertEquals("0", response.getAmountIncludingGst());
    }

    @Test
    void getDuePaymentAmountDetails_shouldApplyGstOnMonthlyPostWhenWithinCollectionRange() {
        DuePaymentAmountDetailsRequest request = new DuePaymentAmountDetailsRequest();
        request.setPaymentAmount("1000");
        request.setGst("10");
        request.setCollectionStartDate(LocalDate.parse("2026-03-01"));
        request.setCollectionEndDate(LocalDate.parse("2026-12-31"));
        request.setPaymentCollectionCycle("monthly");
        request.setPaymentCollectionMode("POST");
        request.setPaymentCapita("PER_FLAT");
        request.setTodayDate(LocalDate.parse("2026-03-15"));

        DuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

        assertEquals(LocalDate.parse("2026-04-01"), response.getDueDate());
        assertEquals("1000", response.getAmountExcludingGst());
        assertEquals("100", response.getGstAmount());
        assertEquals("1100", response.getAmountIncludingGst());
    }
}
