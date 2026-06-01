package com.secura.dnft.interfaceservice;

public interface EmailInterface {

    /**
     * Scheduled job (every day at 12:30 AM) — fetches all Payment, Transaction and
     * Notice entities where emailSentFlag = "N", filters out those already logged in
     * SecuraEmailLog, and sends the appropriate HTML emails.
     */
    void sendEmail();

    /**
     * Scheduled job (every day at 4:00 AM) — re-attempts sending emails for all
     * SecuraEmailLog entries that still have entries in their failedApplicableList.
     */
    void reattemptEmail();

    /**
     * Scheduled job (every day at 5:00 AM) — deletes SecuraEmailLog entries that
     * are older than the configured retention period (default: 90 days) and whose
     * failedApplicableList is empty.
     */
    void deleteOldFailedEmails();
}
