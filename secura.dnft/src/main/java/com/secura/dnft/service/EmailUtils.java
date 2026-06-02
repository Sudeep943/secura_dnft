package com.secura.dnft.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.request.response.PaymentTenderData;

@Service
public class EmailUtils {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

//	@Value("${email.log.retention.days:90}")
//	String payNowUrl;
//	
//	@Value("${email.log.retention.days:90}")
//	 String societyName;
//	  
//	


	public static String getFormatedDate(LocalDate date) {

DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("d-MMM-yyyy");

String formattedDate = date.format(formatter);
return formattedDate;
	}
	
	public static String getTransactionHTMLBody(
			String ownerName,
			String logoBase64,
			String societyName,
			Flat flat,
			Transaction transaction) {
		boolean hasLogo = logoBase64 != null && !logoBase64.isEmpty();

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>");
		html.append("<html>");
		html.append("<head>");
		html.append("<meta charset='UTF-8'>");
		html.append("<title>Transaction Confirmation</title>");
		html.append("</head>");
		html.append("<body style='margin:0;padding:0;background:#eef3ee;font-family:Arial,Helvetica,sans-serif;'>");
		html.append("<table width='100%' bgcolor='#eef3ee' cellpadding='0' cellspacing='0'>");
		html.append("<tr><td align='center' style='padding:30px 10px;'>");
		html.append("<table width='650' cellpadding='0' cellspacing='0' ");
		html.append("style='background:#ffffff;border-radius:12px;border:1px solid #c8e0c8;'>");

		html.append("<tr>");
		html.append("<td style='background:#329932;border-radius:12px 12px 0 0;padding:28px 30px;text-align:center;'>");
		html.append("<h1 style='margin:0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:0.5px;'>")
				.append(safe(societyName))
				.append("</h1>");
		html.append("<p style='margin:6px 0 0;font-size:13px;color:#c8f0c8;letter-spacing:1px;text-transform:uppercase;'>Association of Owners</p>");
		html.append("</td>");
		html.append("</tr>");

		html.append("<tr><td style='padding:28px 30px 10px;'>");
		html.append("<p style='margin:0 0 10px;font-size:15px;color:#333;'>Dear <b>")
				.append(safe(ownerName))
				.append("</b>,</p>");
		html.append("<p style='margin:0;font-size:14px;color:#444;line-height:1.6;'>");
		html.append("Your transaction has been recorded successfully. Please find transaction and flat details below. ");
		html.append("Find detailed receipt of this transaction in the attachment.");
		html.append("</p>");
		html.append("</td></tr>");

		html.append("<tr><td style='padding:20px 30px 10px;'>");
		html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Flat Details</h2>");
		html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
		html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");
		addRow(html, "Flat No.", flat != null ? flat.getFlatNo() : null);
		addRow(html, "Built Up Area", flat != null && flat.getFlatArea() != null ? flat.getFlatArea() + " Sqft" : null);
		html.append("</table>");
		html.append("</td></tr>");

		html.append("<tr><td style='padding:20px 30px 10px;'>");
		html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Transaction Details</h2>");
		html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
		html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");
		addRow(html, "Transaction Date Time", formatTransactionDateTime(transaction != null ? transaction.getTrnsDate() : null));
		addRow(html, "Transaction ID", transaction != null ? transaction.getTrnscId() : null);
		addRow(html, "Invoice number", transaction != null ? transaction.getReceiptNumber() : null);
		addRow(html, "Transaction Tender", transaction != null ? formatTransactionTender(transaction.getTrnsTender()) : null);
		addRow(html, "Amount", transaction != null ? "₹ " + safe(transaction.getTrnsAmt()) : null);
		addRow(html, "Payment Id", transaction != null ? transaction.getPymntId() : null);
		addRow(html, "Transaction Status", transaction != null ? transaction.getTrnsStatus() : null);
		html.append("</table>");
		html.append("</td></tr>");

		html.append("<tr>");
		html.append("<td style='background:#f7faf7;border-top:1px solid #c8e0c8;border-radius:0 0 12px 12px;padding:20px 30px;color:#666;font-size:13px;line-height:1.6;'>");
		html.append("<p style='margin:0 0 4px;'>This is an automated notification. Please do not reply to this email.</p>");
		html.append("<p style='margin:0;'>&nbsp;</p>");
		html.append("<p style='margin:0;'>Thanks &amp; Regards</p>");
		html.append("<p style='margin:2px 0 0;font-weight:600;color:#329932;'>AOA ").append(safe(societyName)).append("</p>");
		if (hasLogo) {
			html.append("<br>");
			html.append("<img src='cid:societylogo' alt='").append(safe(societyName))
					.append("' style='max-height:60px;max-width:160px;margin-top:8px;'>");
		}
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</td></tr>");
		html.append("</table>");
		html.append("</body>");
		html.append("</html>");
		return html.toString();
	}
	
    public static String generatePaymentCollectionEmail(
            String ownerName,          
            String logoBase64,
            String paymentName,
            String shortDesc,
            String cause,
            String startDate,
            String endDate,
            String allowedTenders,
            String unitAmount,
            boolean isPerSqft,
            String gst,
            String paymentType,
            List<DiscFin> discFinList,
            List<DueAmountDetailsEntity> upcomingDues,
            double paymentTotalDue,
            List<DueAmountDetailsEntity> currentPaymentDues,
            String societyName,
            String payNowUrl,
            Flat flat
            
            ) {	

        boolean hasLogo = logoBase64 != null && !logoBase64.isEmpty();

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Payment Notification</title>");
        html.append("</head>");

        html.append("<body style='margin:0;padding:0;background:#eef3ee;font-family:Arial,Helvetica,sans-serif;'>");

        html.append("<table width='100%' bgcolor='#eef3ee' cellpadding='0' cellspacing='0'>");
        html.append("<tr><td align='center' style='padding:30px 10px;'>");

        html.append("<table width='650' cellpadding='0' cellspacing='0' ");
        html.append("style='background:#ffffff;border-radius:12px;border:1px solid #c8e0c8;'>");

        // HEADER
        html.append("<tr>");
        html.append("<td style='background:#329932;border-radius:12px 12px 0 0;padding:28px 30px;text-align:center;'>");
        html.append("<h1 style='margin:0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:0.5px;'>")
                .append(safe(societyName))
                .append("</h1>");
        html.append("<p style='margin:6px 0 0;font-size:13px;color:#c8f0c8;letter-spacing:1px;text-transform:uppercase;'>Association of Owners</p>");
        html.append("</td>");
        html.append("</tr>");

        // GREETING
        html.append("<tr><td style='padding:28px 30px 10px;'>");
        html.append("<p style='margin:0 0 10px;font-size:15px;color:#333;'>Dear <b>")
                .append(safe(ownerName))
                .append("</b>,</p>");
        html.append("<p style='margin:0;font-size:14px;color:#444;line-height:1.6;'>");
        html.append("The AOA has initiated a payment collection for ");
        html.append("<b style='color:#329932;'>").append(safe(paymentName)).append("</b>.");
        html.append(" Please review the details below and complete your payment at the earliest.");
        html.append("</p>");
        html.append("</td></tr>");

        // Flat DETAILS
        html.append("<tr><td style='padding:20px 30px 10px;'>");
        html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Flat Details</h2>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
        html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");
        addRow(html, "Flat No.", flat.getFlatNo());
        if (isPerSqft) {
            addRow(html, "Built Up Area", flat.getFlatArea() + " Sqft");
        }
        html.append("</table>");
        html.append("</td></tr>");

        // PAYMENT DETAILS
        html.append("<tr><td style='padding:20px 30px 10px;'>");
        html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Payment Details</h2>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
        html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");

        addRow(html, "Payment Name", paymentName);
        addRow(html, "Description", shortDesc);
        addRow(html, "Cause", cause);
        addRow(html, "Payment Tenure", startDate + " to " + endDate);
        addRow(html, "Allowed Tenders", allowedTenders);

        String amountDisplay = unitAmount;
        if (isPerSqft) {
            amountDisplay += " Per Sqft";
        }
        addRow(html, "Amount", "₹ " + amountDisplay);
        addRow(html, "GST", gst + "%");
        addRow(html, "Payment Type", paymentType);

        html.append("</table>");
        html.append("</td></tr>");

//        // UPCOMING DUES
//
//        if (upcomingDues != null && !upcomingDues.isEmpty()) {
//
//            html.append("<tr><td style='padding:30px;'>");
//
//            html.append("<h2 style='color:#0d4d8b'>Upcoming Due Details</h2>");
//
//            html.append("<table width='100%' border='1' cellpadding='8' ");
//            html.append("style='border-collapse:collapse;'>");
//
//            html.append("<tr bgcolor='#0d4d8b' style='color:white;'>");
//            html.append("<th>Cycle</th>");
//            html.append("<th>Due Date</th>");
//            html.append("<th>Amount</th>");
//            html.append("<th>Total Amount</th>");
//            html.append("</tr>");
//
//            for (DueAmountDetailsEntity due : upcomingDues) {
//
//                html.append("<tr>");
//
//                html.append("<td>")
//                        .append(safe(due.getCollectionCycle()))
//                        .append("</td>");
//
//                html.append("<td>")
//                        .append(String.valueOf(due.getDueDate()))
//                        .append("</td>");
//
//                html.append("<td>")
//                        .append(safe(due.getAmount()))
//                        .append("</td>");
//
//                html.append("<td>")
//                        .append(safe(due.getTotalAmount()))
//                        .append("</td>");
//
//                html.append("</tr>");
//            }
//
//            html.append("</table>");
//
//            html.append("</td></tr>");
//        }

        // CURRENT DUES
        if (currentPaymentDues != null && !currentPaymentDues.isEmpty()) {

            html.append("<tr><td style='padding:20px 30px 10px;'>");
            html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Current Payment Dues</h2>");
            html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
            html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");

            html.append("<tr style='background:#329932;color:#ffffff;'>");
            html.append("<th style='padding:10px 12px;text-align:left;font-weight:600;'>Cycle</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Due Date</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Amount</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>GST</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Total</th>");
            html.append("</tr>");

            int dueRowIndex = 0;
            for (DueAmountDetailsEntity due : currentPaymentDues) {
                String rowBg = (dueRowIndex % 2 == 0) ? "#ffffff" : "#f2faf2";
                html.append("<tr style='background:").append(rowBg).append(";'>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;color:#333;'>")
                        .append(safe(due.getCollectionCycle()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;color:#333;'>")
                        .append(getFormatedDate(due.getDueDate()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;color:#333;'>")
                        .append("₹ " + safe(due.getAmount()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;color:#333;'>")
                        .append("₹ " + safe(due.getGstAmount()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;font-weight:600;color:#329932;'>")
                        .append("₹ " + safe(due.getTotalAmount()))
                        .append("</td>");
                html.append("</tr>");
                dueRowIndex++;
            }

            html.append("</table>");
            html.append("</td></tr>");
        }

        // DISCOUNTS / FINES
        if (discFinList != null && !discFinList.isEmpty()) {

            html.append("<tr><td style='padding:20px 30px 10px;'>");
            html.append("<h2 style='margin:0 0 10px;font-size:15px;font-weight:700;color:#329932;text-transform:uppercase;letter-spacing:0.5px;border-left:4px solid #329932;padding-left:10px;'>Discounts / Fines</h2>");
            html.append("<table width='100%' cellpadding='0' cellspacing='0' ");
            html.append("style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:6px;overflow:hidden;font-size:14px;'>");

            html.append("<tr style='background:#329932;color:#ffffff;'>");
            html.append("<th style='padding:10px 12px;text-align:left;font-weight:600;'>Type</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Cycle</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Mode</th>");
            html.append("<th style='padding:10px 12px;text-align:center;font-weight:600;'>Value</th>");
            html.append("</tr>");

            int discRowIndex = 0;
            for (DiscFin disc : discFinList) {
                String rowBg = (discRowIndex % 2 == 0) ? "#ffffff" : "#f2faf2";
                html.append("<tr style='background:").append(rowBg).append(";'>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;color:#333;'>")
                        .append(safe(disc.getDiscFnType()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;color:#333;'>")
                        .append(safe(disc.getDiscFnCycleType()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;color:#333;'>")
                        .append(safe(disc.getDiscFnMode()))
                        .append("</td>");
                html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;text-align:center;font-weight:600;color:#329932;'>")
                        .append(safe(disc.getDiscFinValue()))
                        .append("</td>");
                html.append("</tr>");
                discRowIndex++;
            }

            html.append("</table>");
            html.append("</td></tr>");
        }

        // TOTAL DUE
        html.append("<tr>");
        html.append("<td align='center' style='padding:24px 30px 10px;'>");
        html.append("<table cellpadding='0' cellspacing='0' style='display:inline-table;'>");
        html.append("<tr><td style='background:#fff3cd;border:2px solid #e0a800;border-radius:8px;padding:14px 36px;text-align:center;'>");
        html.append("<span style='font-size:13px;color:#666;display:block;margin-bottom:4px;text-transform:uppercase;letter-spacing:0.5px;'>Total Amount Due</span>");
        html.append("<span style='font-size:26px;font-weight:700;color:#c0392b;'>₹ ").append(paymentTotalDue).append("</span>");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</td>");
        html.append("</tr>");

        // PAY BUTTON
        html.append("<tr>");
        html.append("<td align='center' style='padding:20px 30px 30px;'>");
        html.append("<a href='").append(payNowUrl).append("' ");
        html.append("style='display:inline-block;background:#329932;color:#ffffff;font-size:15px;font-weight:700;");
        html.append("padding:14px 48px;text-decoration:none;border-radius:6px;letter-spacing:0.5px;");
        html.append("border:2px solid #267326;'>");
        html.append("PAY NOW");
        html.append("</a>");
        html.append("</td>");
        html.append("</tr>");

        // FOOTER
        html.append("<tr>");
        html.append("<td style='background:#f7faf7;border-top:1px solid #c8e0c8;border-radius:0 0 12px 12px;padding:20px 30px;color:#666;font-size:13px;line-height:1.6;'>");
        html.append("<p style='margin:0 0 4px;'>Dues mentioned are excluding any discount. For exact due amount with discount (if applicable), click on Pay Now and check on the payment page.</p>");
        html.append("<p style='margin:0 0 4px;'>This is an automated notification. Please do not reply to this email.</p>");
        html.append("<p style='margin:0;'>&nbsp;</p>");
        html.append("<p style='margin:0;'>Thanks &amp; Regards</p>");
        html.append("<p style='margin:2px 0 0;font-weight:600;color:#329932;'>AOA ").append(safe(societyName)).append("</p>");
        if (hasLogo) {
            html.append("<br>");
            html.append("<img src='cid:societylogo' alt='").append(safe(societyName))
                    .append("' style='max-height:60px;max-width:160px;margin-top:8px;'>");
        }
        html.append("</td>");
        html.append("</tr>");

        html.append("</table>");
        html.append("</td></tr>");
        html.append("</table>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private static void addRow(StringBuilder html, String label, String value) {
        html.append("<tr>");
        html.append("<td width='38%' style='padding:9px 12px;background:#f2faf2;border-top:1px solid #ddeedd;font-weight:600;color:#4a7a4a;font-size:13px;'>")
                .append(safe(label))
                .append("</td>");
        html.append("<td style='padding:9px 12px;border-top:1px solid #ddeedd;color:#333;font-size:14px;'>")
                .append(safe(value))
                .append("</td>");
        html.append("</tr>");
    }

    private static String formatTransactionDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("d-MMM-yyyy, HH:mm", Locale.ENGLISH));
    }

    private static String formatTransactionTender(String trnsTenderJson) {
    	List<PaymentTenderData> tenderDataList = parseList(trnsTenderJson, new TypeReference<List<PaymentTenderData>>() {});
    	if (!tenderDataList.isEmpty()) {
    		String tenderNames = tenderDataList.stream()
    				.filter(Objects::nonNull)
    				.map(PaymentTenderData::getTenderName)
    				.filter(Objects::nonNull)
    				.map(tenderName -> tenderName.replace('_', ' ').trim())
    				.filter(tenderName -> !tenderName.isEmpty())
    				.collect(Collectors.joining(", "));
    		if (!tenderNames.isEmpty()) {
    			return tenderNames;
    		}
    	}
    	return trnsTenderJson;
    }

    private static <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
    	if (json == null || json.isBlank()) {
    		return new ArrayList<>();
    	}
    	try {
    		return OBJECT_MAPPER.readValue(json, typeReference);
    	} catch (JsonProcessingException e) {
    		return new ArrayList<>();
    	}
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}