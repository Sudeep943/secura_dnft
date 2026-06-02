package com.secura.dnft.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;

@Service
public class EmailUtils {

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

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Payment Notification</title>");
        html.append("</head>");

        html.append("<body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>");

        html.append("<table width='100%' bgcolor='#f4f6f9' cellpadding='20'>");
        html.append("<tr><td align='center'>");

        html.append("<table width='800' cellpadding='0' cellspacing='0' ");
        html.append("style='background:white;border-radius:10px;'>");

        // HEADER

        html.append("<tr>");
        html.append("<td style='background:#329932;color:white;padding:25px;text-align:center;'>");

        if (logoBase64 != null && !logoBase64.isEmpty()) {
            html.append("<img src='data:image/png;base64,")
                    .append(logoBase64)
                    .append("' style='max-height:80px'><br><br>");
        }

        html.append("<h1 style='margin:0;'>")
                .append(societyName)
                .append("</h1>");

        html.append("<p>Association of Owners</p>");

        html.append("</td>");
        html.append("</tr>");

        // GREETING

        html.append("<tr><td style='padding:30px;'>");

        html.append("<p>Dear <b>")
                .append(ownerName)
                .append("</b>,</p>");

        html.append("<p>");
        html.append("AOA of society has initiated a collection of payment ");
        html.append("<b>").append(paymentName).append("</b>.");
        html.append("</p>");

        html.append("</td></tr>");
        // Flat DETAILS

        html.append("<tr><td style='padding:0 30px;'>");

        html.append("<h2 style='color:#329932'>Flat Details</h2>");

        html.append("<table width='100%' border='1' cellpadding='8' ");
        html.append("style='border-collapse:collapse;'>");

        addRow(html, "Flat", flat.getFlatNo());
        if (isPerSqft) {
        addRow(html, "Built Up Area", flat.getFlatArea()+ " Sqft");
        }
        html.append("</table>");

        html.append("</td></tr>");
        
        // PAYMENT DETAILS

        html.append("<tr><td style='padding:0 30px;'>");

        html.append("<h2 style='color:#329932'>Payment Details</h2>");

        html.append("<table width='100%' border='1' cellpadding='8' ");
        html.append("style='border-collapse:collapse;'>");

        addRow(html, "Payment Name", paymentName);
        addRow(html, "Description", shortDesc);
        addRow(html, "Cause", cause);
        addRow(html, "Payment Tenure", startDate + " to " + endDate);
        addRow(html, "Allowed Tenders", allowedTenders);

        String amountDisplay = unitAmount;

        if (isPerSqft) {
            amountDisplay += " Per Sqft";
        }

        addRow(html, "Amount", "₹ "+amountDisplay);
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

            html.append("<tr><td style='padding:30px;'>");

            html.append("<h2 style='color:#329932'>Current Payment Dues</h2>");

            html.append("<table width='100%' border='1' cellpadding='8' ");
            html.append("style='border-collapse:collapse;'>");

            html.append("<tr bgcolor='#329932' style='color:white;'>");
            html.append("<th>Cycle</th>");
            html.append("<th>Due Date</th>");
            html.append("<th>Amount</th>");
            html.append("<th>GST</th>");
            html.append("<th>Total</th>");
            html.append("</tr>");

            for (DueAmountDetailsEntity due : currentPaymentDues) {

                html.append("<tr>");

                html.append("<td>")
                        .append(safe(due.getCollectionCycle()))
                        .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append(getFormatedDate(due.getDueDate()))
                        .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append("₹ "+safe(due.getAmount()))
                        .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append("₹ "+safe(due.getGstAmount()))
                        .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append("₹ "+safe(due.getTotalAmount()))
                        .append("</td>");

                html.append("</tr>");
            }

            html.append("</table>");

            html.append("</td></tr>");
        }

        // DISCOUNTS / FINES
        if (discFinList != null && !discFinList.isEmpty()) {

            html.append("<tr><td style='padding:30px;'>");

            html.append("<h2 style='color:#329932'>Discounts / Fines</h2>");

            html.append("<table width='100%' border='1' cellpadding='8' ");
            html.append("style='border-collapse:collapse;'>");

            html.append("<tr bgcolor='#329932' style='color:white;'>");
            html.append("<th>Type</th>");
            html.append("<th>Cycle</th>");
            html.append("<th>Mode</th>");
            html.append("<th>Value</th>");
            html.append("</tr>");

            for (DiscFin disc : discFinList) {

                html.append("<tr>");

                html.append("<td>")
                        .append(safe(disc.getDiscFnType()))
                        .append("</td>");
                html.append("<td style=\"text-align:center;\">")
                .append(safe(disc.getDiscFnCycleType()))
                .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append(safe(disc.getDiscFnMode()))
                        .append("</td>");

                html.append("<td style=\"text-align:center;\">")
                        .append(safe(disc.getDiscFinValue()))
                        .append("</td>");

                html.append("</tr>");
            }

            html.append("</table>");

            html.append("</td></tr>");
        }

        // TOTAL DUE

        html.append("<tr>");
        html.append("<td align='center' style='padding:25px;'>");

        html.append("<h2 style='color:red;'>");
        html.append("Total Due : ₹ ").append(paymentTotalDue);
        html.append("</h2>");

        html.append("</td>");
        html.append("</tr>");

        // PAY BUTTON

        html.append("<tr>");
        html.append("<td align='center' style='padding-bottom:30px;'>");

        html.append("<a href='")
                .append(payNowUrl)
                .append("' ");

        html.append("style='background:#28a745;");
        html.append("color:white;");
        html.append("padding:15px 40px;");
        html.append("text-decoration:none;");
        html.append("border-radius:5px;'>");

        html.append("PAY NOW");

        html.append("</a>");

        html.append("</td>");
        html.append("</tr>");

        // FOOTER

        html.append("<tr>");
        html.append("<td style='padding:20px;color:#666;'>");

        html.append("This is an automated notification.");
        html.append("<br>");
        html.append("Please do not reply to this email.");
        html.append("<br><br>");

        html.append("Thanks & Regards");
        html.append("<br>");
        html.append("AOA ");
        html.append(societyName);

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

        html.append("<td width='35%'><b>")
                .append(label)
                .append("</b></td>");

        html.append("<td>")
                .append(safe(value))
                .append("</td>");

        html.append("</tr>");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}