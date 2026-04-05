package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.interfaceservice.PaymentInterface;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.UpdatePaymentRequest;
import com.secura.dnft.request.response.UpdatePaymentResponse;

@Service
public class PaymentServices  implements PaymentInterface{

	
	@Autowired
	GenericService genericService;
	
	@Autowired
	PaymentRepository paymentRepository;
	
	@Override
    public DuePaymentAmountDetailsResponse getDuePaymentAmountDetails(DuePaymentAmountDetailsRequest request) {
        DuePaymentAmountDetailsResponse response = new DuePaymentAmountDetailsResponse();
        response.setGenericHeader(request.getGenericHeader());
        response.setPaymentCapita(request.getPaymentCapita());

        if (request.getCollectionStartDate() == null || request.getCollectionEndDate() == null || request.getTodayDate() == null) {
            return response;
        }

        int cycleMonths = getCycleMonths(request.getPaymentCollectionCycle());
        if (cycleMonths <= 0) {
            return response;
        }

        LocalDate dueDate = calculateDueDate(request.getCollectionStartDate(), request.getCollectionEndDate(),
                request.getTodayDate(), request.getPaymentCollectionMode(), cycleMonths);
        response.setDueDate(dueDate);

        BigDecimal cycleAmount = parseNumeric(request.getPaymentAmount());
        BigDecimal gstPercent = parseNumeric(request.getGst());

        BigDecimal dueBaseAmount = calculateDueBaseAmount(dueDate, cycleMonths, request.getCollectionEndDate(), cycleAmount);
        BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalWithGst = dueBaseAmount.add(gstAmount);

        response.setAmountExcludingGst(formatNumber(dueBaseAmount));
        response.setGstPercent(formatNumber(gstPercent));
        response.setGstAmount(formatNumber(gstAmount));
        response.setAmountIncludingGst(formatNumber(totalWithGst));

        return response;
    }

    private LocalDate calculateDueDate(LocalDate start, LocalDate end, LocalDate today, String mode, int cycleMonths) {
        LocalDate periodStart = start;
        while (!periodStart.isAfter(end)) {
            LocalDate naturalPeriodEnd = periodStart.plusMonths(cycleMonths).minusDays(1);
            LocalDate periodEnd = naturalPeriodEnd.isAfter(end) ? end : naturalPeriodEnd;
            if (!today.isBefore(periodStart) && !today.isAfter(periodEnd)) {
                if (isPost(mode)) {
                    return periodEnd.plusDays(1);
                }
                return periodStart;
            }
            periodStart = periodStart.plusMonths(cycleMonths);
        }
        if (isPost(mode)) {
            if (today.isAfter(end)) {
                return end.plusDays(1);
            }
            return start.plusMonths(cycleMonths);
        }
        return start;
    }

    private BigDecimal calculateDueBaseAmount(LocalDate dueDate, int cycleMonths, LocalDate collectionEndDate,
            BigDecimal cycleAmount) {
        LocalDate naturalCycleEnd = dueDate.plusMonths(cycleMonths).minusDays(1);
        if (!naturalCycleEnd.isAfter(collectionEndDate)) {
            return cycleAmount.setScale(2, RoundingMode.HALF_UP);
        }

        long totalCycleDays = ChronoUnit.DAYS.between(dueDate, naturalCycleEnd.plusDays(1));
        long activeCycleDays = ChronoUnit.DAYS.between(dueDate, collectionEndDate.plusDays(1));
        if (activeCycleDays < 0) {
            activeCycleDays = 0;
        }
        if (activeCycleDays > totalCycleDays) {
            activeCycleDays = totalCycleDays;
        }
        if (totalCycleDays == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return cycleAmount.multiply(BigDecimal.valueOf(activeCycleDays))
                .divide(BigDecimal.valueOf(totalCycleDays), 2, RoundingMode.HALF_UP);
    }

    private int getCycleMonths(String cycle) {
        if (cycle == null) {
            return 0;
        }
        String normalized = cycle.trim().toLowerCase();
        if (normalized.equals("monthly")) {
            return 1;
        }
        if (normalized.equals("quarterly") || normalized.equals("quaterly")) {
            return 3;
        }
        if (normalized.equals("halfyearly") || normalized.equals("half yearly")) {
            return 6;
        }
        if (normalized.equals("yearly")) {
            return 12;
        }
        return 0;
    }

    private boolean isPost(String mode) {
        return mode != null && mode.trim().equalsIgnoreCase("post");
    }

    private BigDecimal parseNumeric(String input) {
        if (input == null || input.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String normalized = input.replace("%", "").replace(",", "").trim();
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatNumber(BigDecimal value) {
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return normalized.toPlainString();
    }

    @Override
	public UpdatePaymentResponse updatePayment(UpdatePaymentRequest request) throws Exception {
		UpdatePaymentResponse response = new UpdatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		return response;
	}

	@Override
	public GetPaymentResponse getPayments(GetPaymentRequest request) throws Exception {
		GetPaymentResponse response = new GetPaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		return response;
	}


	@Override
	public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws Exception {
		CreatePaymentResponse response = new CreatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		PaymentEntity entity= new PaymentEntity();
		
		entity.setPaymentName(request.getPaymentName());
		entity.setShortDetails(request.getShortDetails());
		entity.setPaymentCapita(request.getPaymentCapita());
		entity.setPaymentAmount(request.getPaymentAmount());
		entity.setGst(request.getGst());
		entity.setCurrency(SecuraConstants.PAYMENT_CURRENCY);
		entity.setCollectionStartDate(genericService.getCorrectLocalDateForInputDate(request.getCollectionStartDate()));
		entity.setCollectionEndDate(genericService.getCorrectLocalDateForInputDate(request.getCollectionEndDate()));
		entity.setPaymentCollectionCycle(request.getPaymentCollectionCycle());
		entity.setPaymentCollectionMode(request.getPaymentCollectionMode());
		entity.setApplicableFor(request.getApplicableFor());
		entity.setPaymentType(request.getPaymentType());
		entity.setBankAccountId(request.getBankAccountId());
		entity.setStatus(SecuraConstants.PAYMENT_STATUS_CREATED);
		
		return response;
	}


}
