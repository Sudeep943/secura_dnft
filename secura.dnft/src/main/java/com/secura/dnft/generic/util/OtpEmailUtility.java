package com.secura.dnft.generic.util;

/**
 * Generic utility for building OTP-related email bodies.
 * All OTP transaction emails share the same branded HTML template.
 */
public final class OtpEmailUtility {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private OtpEmailUtility() {
        // utility class – no instantiation
    }

    /**
     * Builds a themed HTML email body for OTP delivery.
     *
     * @param otp   the raw 6-digit OTP to embed in the email
     * @param otpId the unique OTP reference identifier to display alongside the OTP
     * @return complete HTML string ready to be sent as the email body
     */
    public static String buildOtpEmailBody(String otp, String otpId) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'><title>Secura OTP Verification</title></head>"
                + "<body style='margin:0;padding:0;background:#eef3ee;font-family:Arial,Helvetica,sans-serif;'>"
                + "<table width='100%' bgcolor='#eef3ee' cellpadding='0' cellspacing='0'>"
                + "<tr><td align='center' style='padding:30px 10px;'>"
                + "<table width='600' cellpadding='0' cellspacing='0' "
                + "style='background:#ffffff;border-radius:12px;border:1px solid #c8e0c8;'>"

                // ── Header ────────────────────────────────────────────────────────────────
                + "<tr>"
                + "<td style='background:#00A696;border-radius:12px 12px 0 0;padding:28px 30px;text-align:center;'>"
                + "<h1 style='margin:0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:0.5px;'>Secura</h1>"
                + "<p style='margin:6px 0 0;font-size:13px;color:#c8f0c8;letter-spacing:1px;text-transform:uppercase;'>Secure Society Management</p>"
                + "</td>"
                + "</tr>"

                // ── Body ──────────────────────────────────────────────────────────────────
                + "<tr><td style='padding:28px 30px 10px;'>"
                + "<p style='margin:0 0 10px;font-size:15px;color:#333;'>Dear User,</p>"
                + "<p style='margin:0;font-size:14px;color:#444;line-height:1.6;'>"
                + "Please use the One-Time Password (OTP) below to complete your verification. "
                + "This OTP is valid for <strong>" + OTP_EXPIRY_MINUTES + " minutes</strong> and must not be shared with anyone."
                + "</p>"
                + "</td></tr>"

                // ── OTP box ───────────────────────────────────────────────────────────────
                + "<tr><td style='padding:20px 30px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' "
                + "style='border-collapse:collapse;border:1px solid #c8e0c8;border-radius:8px;overflow:hidden;'>"
                + "<tr>"
                + "<td style='background:#f5faf5;padding:18px 24px;border-bottom:1px solid #c8e0c8;'>"
                + "<span style='font-size:12px;color:#00A696;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Your OTP</span><br/>"
                + "<span style='font-size:36px;font-weight:700;letter-spacing:10px;color:#00A696;'>" + otp + "</span>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='background:#ffffff;padding:12px 24px;'>"
                + "<span style='font-size:12px;color:#777;text-transform:uppercase;letter-spacing:0.5px;'>OTP Reference ID</span><br/>"
                + "<span style='font-size:13px;font-weight:600;color:#333;'>" + otpId + "</span>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</td></tr>"

                // ── Warning ───────────────────────────────────────────────────────────────
                + "<tr><td style='padding:0 30px 20px;'>"
                + "<p style='font-size:13px;color:#888;line-height:1.6;margin:0;'>"
                + "If you did not request this OTP, please ignore this email or contact your society administrator immediately."
                + "</p>"
                + "</td></tr>"

                // ── Footer ────────────────────────────────────────────────────────────────
                + "<tr>"
                + "<td style='background:#f5faf5;border-radius:0 0 12px 12px;padding:16px 30px;text-align:center;border-top:1px solid #c8e0c8;'>"
                + "<p style='margin:0;font-size:11px;color:#aaa;'>This is an automated message. Please do not reply to this email.</p>"
                + "<p style='margin:4px 0 0;font-size:11px;color:#aaa;'>&copy; Secura Society Management Platform</p>"
                + "</td>"
                + "</tr>"

                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body></html>";
    }
}
