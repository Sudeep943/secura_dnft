package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;

@Service
public class PaymentServices {

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
        BigDecimal totalWithGst = dueBaseAmount.add(dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        response.setAmountExcludingGst(formatNumber(dueBaseAmount));
        response.setGstPercent(formatNumber(gstPercent));
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
                    LocalDate nextPeriodStart = periodStart.plusMonths(cycleMonths);
                    return nextPeriodStart.isAfter(end) ? periodStart : nextPeriodStart;
                }
                return periodStart;
            }
            periodStart = periodStart.plusMonths(cycleMonths);
        }
        return isPost(mode) ? start.plusMonths(cycleMonths) : start;
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
}
