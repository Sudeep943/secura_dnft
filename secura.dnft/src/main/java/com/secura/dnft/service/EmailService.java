package com.secura.dnft.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.NoticeRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.ReceiptRepository;
import com.secura.dnft.dao.SecuraEmailLogRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.FailedEmailCause;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.NoticeEntity;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Receipt;
import com.secura.dnft.entity.SecuraEmailLog;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.interfaceservice.EmailInterface;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements EmailInterface {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String EMAIL_LOG_TYPE_PAYMENT     = "Payment";
    private static final String EMAIL_LOG_TYPE_TRANSACTION = "Transaction";
    private static final String EMAIL_LOG_TYPE_NOTICE      = "Notice";
    private static final String EMAIL_LOG_TYPE_BOOKING     = "Booking";

    private static final String EMAIL_SENT_FLAG_NO = "N";
    private static final String EMAIL_SENT_FLAG_YES = "Y";

    private static final String CYCLE_YEARLY       = "YEARLY";
    private static final String CYCLE_HALF_YEARLY  = "HALF_YEARLY";
    private static final String CYCLE_QUARTERLY    = "QUARTERLY";
    private static final String CYCLE_MONTHLY      = "MONTHLY";

    private static final List<String> CYCLE_PRIORITY = Arrays.asList(
            CYCLE_YEARLY, CYCLE_HALF_YEARLY, CYCLE_QUARTERLY, CYCLE_MONTHLY
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d-MMM-yyyy");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${email.log.retention.days:90}")
    private int emailLogRetentionDays;
    
    
    @Value("${app.payment.url}")
    private String paymentURL;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private SecuraEmailLogRepository emailLogRepository;

    @Autowired
    private FlatRepository flatRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DiscFinRepository discFinRepository;

    @Autowired
    private DueAmountDetailsRepository dueAmountDetailsRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private FlatServices flatServices;
    
    @Autowired
    GenericService genericService;
    
    @Autowired
    EmailUtils emailUtils;

    // -------------------------------------------------------------------------
    // Scheduled Jobs
    // -------------------------------------------------------------------------

    @Override
    //@Scheduled(cron = "0 30 0 * * *")
    @Scheduled(cron = "0 */1 * * * *")
    public void sendEmail() {
        logger.info("EmailService.sendEmail() started");
        try {
            List<PaymentEntity> allPendingPayments     = paymentRepository.findByEmailSentflag(EMAIL_SENT_FLAG_NO);
            List<Transaction>   allPendingTransactions = transactionRepository.findByEmailSentflag(EMAIL_SENT_FLAG_NO);
            List<NoticeEntity>  allPendingNotices      = noticeRepository.findByEmailSentflag(EMAIL_SENT_FLAG_NO);

            List<PaymentEntity> filteredPayments     = filterPaymentList(allPendingPayments);
            List<Transaction>   filteredTransactions = filterTransactionList(allPendingTransactions);
            List<NoticeEntity>  filteredNotices      = filterNoticeList(allPendingNotices);

            sendPaymentMails(filteredPayments);
            sendTransactionMails(filteredTransactions);
            //sendNoticeMails(filteredNotices);
        } catch (Exception e) {
            logger.error("EmailService.sendEmail() encountered an error", e);
        }
        logger.info("EmailService.sendEmail() completed");
    }

    @Override
    @Scheduled(cron = "0 0 4 * * *")
    public void reattemptEmail() {
        logger.info("EmailService.reattemptEmail() started");
        try {
            List<SecuraEmailLog> logs = emailLogRepository.findByFailedApplicableListIsNotNull();
            for (SecuraEmailLog log : logs) {
                if (log.getFailedApplicableList() == null || log.getFailedApplicableList().isBlank()) {
                    continue;
                }
                List<String> failedFlats = parseStringList(log.getFailedApplicableList());
                if (failedFlats.isEmpty()) {
                    continue;
                }
                if (EMAIL_LOG_TYPE_PAYMENT.equals(log.getType())) {
                    Optional<PaymentEntity> optPayment = paymentRepository
                            .findFirstByPaymentId(log.getReferenceUniqueId());
                    optPayment.ifPresent(payment -> retryPaymentEmailForFlats(payment, failedFlats, log));
                } else if (EMAIL_LOG_TYPE_TRANSACTION.equals(log.getType())) {
                    retryTransactionEmail(log);
                } else if (EMAIL_LOG_TYPE_NOTICE.equals(log.getType())) {
                    retryNoticeEmail(log);
                }
            }
        } catch (Exception e) {
            logger.error("EmailService.reattemptEmail() encountered an error", e);
        }
        logger.info("EmailService.reattemptEmail() completed");
    }

    @Override
    @Scheduled(cron = "0 0 5 * * *")
    public void deleteOldFailedEmails() {
        logger.info("EmailService.deleteOldFailedEmails() started");
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(emailLogRetentionDays);
            List<SecuraEmailLog> oldLogs = emailLogRepository.findByCreateTsBefore(cutoff);
            List<SecuraEmailLog> toDelete = oldLogs.stream()
                    .filter(log -> log.getFailedApplicableList() == null || log.getFailedApplicableList().isBlank())
                    .collect(Collectors.toList());
            if (!toDelete.isEmpty()) {
                emailLogRepository.deleteAll(toDelete);
                logger.info("Deleted {} old email log entries", toDelete.size());
            }
        } catch (Exception e) {
            logger.error("EmailService.deleteOldFailedEmails() encountered an error", e);
        }
        logger.info("EmailService.deleteOldFailedEmails() completed");
    }

    // -------------------------------------------------------------------------
    // Filter helpers
    // -------------------------------------------------------------------------

    private List<PaymentEntity> filterPaymentList(List<PaymentEntity> paymentList) {
        return paymentList.stream()
                .filter(p -> emailLogRepository
                        .findByTypeAndReferenceUniqueId(EMAIL_LOG_TYPE_PAYMENT, p.getPaymentId())
                        .isEmpty())
                .collect(Collectors.toList());
    }

    private List<Transaction> filterTransactionList(List<Transaction> transactionList) {
        return transactionList.stream()
                .filter(t -> emailLogRepository
                        .findByTypeAndReferenceUniqueId(EMAIL_LOG_TYPE_TRANSACTION, t.getTrnscId())
                        .isEmpty())
                .collect(Collectors.toList());
    }

    private List<NoticeEntity> filterNoticeList(List<NoticeEntity> noticeList) {
        return noticeList.stream()
                .filter(n -> emailLogRepository
                        .findByTypeAndReferenceUniqueId(EMAIL_LOG_TYPE_NOTICE, n.getNoticeId())
                        .isEmpty())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // sendPaymentMails
    // -------------------------------------------------------------------------

    List<Profile> getOwnerProfiles(List<String> ownerIds){
    	 List<Profile> profiles = new ArrayList<>();
    	 for(String owner: ownerIds) {
//    		 List<String>ownerProfiles=genericService.fromJson(owner.getPrflId(), new TypeReference<List<String>>() {
//  			});
    		 Optional<Profile> profile= profileRepository.findById(owner);
    		 if(profile.isPresent()) {
    			 profiles.add(profile.get());
    		 }
    	 }
    	 return profiles;
    } 
    
  
    private void sendPaymentMails(List<PaymentEntity> paymentEntityList) {
        for (PaymentEntity payment : paymentEntityList) {
            SecuraEmailLog emailLog = createInitialEmailLog(EMAIL_LOG_TYPE_PAYMENT, payment.getPaymentId());
            List<String> failedFlatList = new ArrayList<>();
            List<FailedEmailCause> failedCauses = new ArrayList<>();
            int emailSentCount = 0;

            List<String> applicableFlatList = getApplicableFlats(payment);
            emailLog.setTotalApplicable(applicableFlatList.size());

            for (String flatId : applicableFlatList) {
                try {
                    Optional<Flat> optFlat = flatRepository.findById(flatId);
                    if (optFlat.isEmpty()) {
                        continue;
                    }
                    Flat flat = optFlat.get();

                    // a.4 Get owner list and email list
                   // List<Owner> owners = ownerRepository.findByFlatNo(flatId).stream().filter(o->o.getStatus().equalsIgnoreCase("ACTIVE")).collect(Collectors.toList());
                    List<String> ownerProfiles=genericService.fromJson(flat.getFlatOwnerList(), new TypeReference<List<String>>() {
          			});
                    
                    List<Profile> profiles=getOwnerProfiles(ownerProfiles);
                    List<String> emailList = profiles.stream()
                            .map(Profile::getPrflEmailAdrss)
                            .filter(e -> e != null && !e.isBlank())
                            .collect(Collectors.toList());
                   // emailList.add("sudeepmishrafanmail@gmail.com");
                    if (emailList.isEmpty()) {
                        continue;
                    }

                    // a.5 Construct owner name
                    String ownerName = buildOwnerNames(profiles);

                    // a.6 Call getDueAmountForFlat
                    GetDueAmountForFlatRequest dueRequest = new GetDueAmountForFlatRequest();
                    GenericHeader header = new GenericHeader();
                    header.setApartmentId(payment.getAprmtId());
                    dueRequest.setGenericHeader(header);
                    dueRequest.setFlatId(flatId);
                    GetDueAmountForFlatResponse dueResponse = flatServices.getDueAmountForFlat(dueRequest);

                    // a.7 Filter due for current payment
                    List<DueAmountDetailsEntity> currentPaymentDues = extractDuesForPayment(dueResponse, payment.getPaymentId());

                    // a.8 Payment details
                    String paymentName      = payment.getPaymentName();
                    String startDate        = payment.getCollectionStartDate() != null ? payment.getCollectionStartDate().format(DATE_FMT) : "-";
                    String endDate          = payment.getCollectionEndDate() != null ? payment.getCollectionEndDate().format(DATE_FMT) : "-";
                    String shortDesc        = payment.getShortDetails();
                    String cause            = payment.getCauseId();
                    String paymentCapita    = payment.getPaymentCapita();
                    List<String> allowedTendersList   =genericService.fromJson( payment.getAllowedPaymentModes(), new TypeReference<List<String>>() {
          			});
                    String allowedTenders= String.join(",", allowedTendersList);
                    allowedTenders=allowedTenders.replace("_", " ");
                    String unitAmount       = payment.getPaymentAmount();
                    String gst              = payment.getGst();
                    String paymentType      = payment.getPaymentType();
                    boolean isPerSqft       = "PER_SQFT".equalsIgnoreCase(paymentCapita);

                    // a.9 Fetch discount details
                    List<String> discFinCodes = parseStringList(payment.getDiscFin());
                    List<DiscFin> discFinList = new ArrayList<>();
                    for (String code : discFinCodes) {
                    	List<Map<String, String>>discFin=genericService.fromJson(code, new TypeReference<List<Map<String, String>>>() {
            			});
                    	for(Map<String, String> map:discFin) {
                    		if(map.get("Status").equalsIgnoreCase(SecuraConstants.DISC_FIN_STATUS_ACTIVE)) {
                    			List<DiscFin> found = discFinRepository.findByDiscFnId(map.get("code"));
                                found.stream()
                                        .filter(d -> "DISCOUNT".equalsIgnoreCase(d.getDiscFnType()))
                                        .forEach(discFinList::add);
                                break;
                    		}
                    	}
//                    	Optional<Map<String, String>>activeDiscfin=discFin.stream().map(dis->).filter(dis->dis.get("status").equals(SecuraConstants.DISC_FIN_STATUS_ACTIVE)).findFirst();
//                    	if(activeDiscfin.isPresent()) {
//                    		List<DiscFin> found = discFinRepository.findByDiscFnId(activeDiscfin.get().get("code"));
//                            found.stream()
//                                    .filter(d -> "DISCOUNT".equalsIgnoreCase(d.getDiscFnType()))
//                                    .forEach(discFinList::add);
//                    	}
                        
                    }

                    // a.11-a.12 Fetch all dues for payment, find highest cycle
                    List<DueAmountDetailsEntity> allDues = dueAmountDetailsRepository.findByPaymentId(payment.getPaymentId());
                    List<DueAmountDetailsEntity> flatDues = allDues.stream()
                            .filter(d -> isPerSqft
                                    ? flat.getFlatArea() != null && flat.getFlatArea().equals(d.getFlatArea())
                                    : true)
                            .collect(Collectors.toList());

                    // a.11 Find highest cycle dues (Yearly > Half Yearly > Quarterly > Monthly)
                    List<DueAmountDetailsEntity> highestCycleDues = findHighestCycleDues(flatDues);

                    // a.13 Accumulate total amount
                    double paymentTotalDue = highestCycleDues.stream()
                            .mapToDouble(d -> parseDouble(d.getTotalAmount()))
                            .sum();

                    // a.14 Filter dues: flat id is applicable but not in paidFlatList
                    List<String> paidFlatList = parseStringList(payment.getPaidFlats());
                    List<DueAmountDetailsEntity> upcomingDues = highestCycleDues.stream()
                            .filter(d -> !paidFlatList.contains(flatId))
                            .collect(Collectors.toList());

                    // Upcoming dues closest to today
                    List<DueAmountDetailsEntity> upcomingDuesByDate = getUpcomingDuesSortedByDate(upcomingDues);

                    // a.15 Get apartment logo
                    //String logoBase64 = getApartmentLogo(payment.getAprmtId());
                    Optional<ApartmentMaster> opt = apartmentRepository.findById(payment.getAprmtId());
                    String logoBase64=opt.map(ApartmentMaster::getAprmnt_logo).orElse(null);
                    String apartName=opt.map(ApartmentMaster::getAprmntName).orElse(null);
                    // Build HTML email body
//                    String htmlBody = buildPaymentEmailHtml(
//                            ownerName, logoBase64,
//                            paymentName, shortDesc, cause, startDate, endDate,
//                            allowedTenders, unitAmount, isPerSqft, gst, paymentType,
//                            discFinList, upcomingDuesByDate, paymentTotalDue,
//                            currentPaymentDues
//                    );
                    String htmlBody= emailUtils.generatePaymentCollectionEmail(ownerName, logoBase64, paymentName, shortDesc, cause, startDate, endDate, 
                    		allowedTenders, unitAmount, isPerSqft, gst, paymentType, discFinList, upcomingDuesByDate, paymentTotalDue, currentPaymentDues,apartName,paymentURL,flat);
                    String subject = "Due Created For " + paymentName;

                    // Send email
                    boolean sentToFlat = false;
                    for (String email : emailList) {
                        sendHtmlEmailWithLogo(email, subject, htmlBody, logoBase64);
                        sentToFlat = true;
                    }
                    if (sentToFlat) {
                        emailSentCount++;
                    }

                } catch (Exception e) {
                    logger.error("Failed to send payment email for flat {}: {}", flatId, e.getMessage(), e);
                    failedFlatList.add(flatId);
                    FailedEmailCause failedCause = new FailedEmailCause(
                            payment.getPaymentId(), EMAIL_LOG_TYPE_PAYMENT, e.getMessage(), flatId
                    );
                    failedCauses.add(failedCause);
                }
            }

            // Update email log
            if(failedFlatList.size()>0) {
            emailLog.setEmailSent(emailSentCount);
            emailLog.setFailedApplicableList(toJson(failedFlatList));
            emailLog.setFailedEmailCause(toJson(failedCauses));
            emailLogRepository.save(emailLog);
            }

            // Update emailSentFlag on payment if all emails sent
            if (failedFlatList.isEmpty()) {
                payment.setEmailSentflag(EMAIL_SENT_FLAG_YES);
                paymentRepository.save(payment);
            }
        }
    }

    // -------------------------------------------------------------------------
    // sendTransactionMails
    // -------------------------------------------------------------------------

    private void sendTransactionMails(List<Transaction> transactionList) {
        for (Transaction transaction : transactionList) {
            SecuraEmailLog emailLog = createInitialEmailLog(EMAIL_LOG_TYPE_TRANSACTION, transaction.getTrnscId());
            List<FailedEmailCause> failedCauses = new ArrayList<>();
            int emailSentCount = 0;

            try {
                String flatId = transaction.getFlatId();
               // List<Owner> owners = flatId != null ? ownerRepository.findByFlatNo(flatId) : new ArrayList<>();
                Optional<Flat> optFlat = flatRepository.findById(flatId);
                if (optFlat.isEmpty()) {
                    continue;
                }
                Flat flat = optFlat.get();
                List<String> ownerProfiles=genericService.fromJson(flat.getFlatOwnerList(), new TypeReference<List<String>>() {
      			});
                List<Profile> profiles = ownerProfiles.stream()
                        .map(o -> profileRepository.findById(o))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                List<String> emailList = profiles.stream()
                        .map(Profile::getPrflEmailAdrss)
                        .filter(e -> e != null && !e.isBlank())
                        .collect(Collectors.toList());

                emailLog.setTotalApplicable(emailList.size());

                String ownerName = buildOwnerNames(profiles);
                Optional<ApartmentMaster> apartmentOpt = apartmentRepository.findById(transaction.getAprmntId());
                String logoBase64 = apartmentOpt.map(ApartmentMaster::getAprmnt_logo).orElse(null);
                String societyName = apartmentOpt.map(ApartmentMaster::getAprmntName).orElse("");

                String htmlBody = emailUtils.getTransactionHTMLBody(ownerName, logoBase64, societyName, flat, transaction);
                String subject = "Transaction Confirmation - " + transaction.getTrnscId();
                byte[] receiptPdfBytes = getReceiptPdfBytes(transaction);
                String attachmentName = getReceiptAttachmentName(transaction);

                for (String email : emailList) {
                    sendHtmlEmailWithLogoAndAttachment(email, subject, htmlBody, logoBase64, receiptPdfBytes, attachmentName);
                }
                if (!emailList.isEmpty()) {
                    emailSentCount++;
                }

                transaction.setEmailSentflag(EMAIL_SENT_FLAG_YES);
                transactionRepository.save(transaction);

            } catch (Exception e) {
                logger.error("Failed to send transaction email for {}: {}", transaction.getTrnscId(), e.getMessage(), e);
                FailedEmailCause failedCause = new FailedEmailCause(
                        transaction.getTrnscId(), EMAIL_LOG_TYPE_TRANSACTION, e.getMessage(), transaction.getTrnscId()
                );
                failedCauses.add(failedCause);
            }

            emailLog.setEmailSent(emailSentCount);
            emailLog.setFailedEmailCause(toJson(failedCauses));
            emailLogRepository.save(emailLog);
        }
    }

    // -------------------------------------------------------------------------
    // sendNoticeMails
    // -------------------------------------------------------------------------

    private void sendNoticeMails(List<NoticeEntity> noticeList) {
        for (NoticeEntity notice : noticeList) {
            SecuraEmailLog emailLog = createInitialEmailLog(EMAIL_LOG_TYPE_NOTICE, notice.getNoticeId());
            List<FailedEmailCause> failedCauses = new ArrayList<>();
            int emailSentCount = 0;

            try {
                // Send notice to all flat owners in the apartment
                List<Flat> flats = flatRepository.findByAprmntId(notice.getAprmtId());
                List<String> emailList = new ArrayList<>();
                for (Flat flat : flats) {
                    List<Owner> owners = ownerRepository.findByFlatNo(flat.getFlatNo());
                    owners.stream()
                            .map(o -> profileRepository.findById(o.getPrflId()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(Profile::getPrflEmailAdrss)
                            .filter(e -> e != null && !e.isBlank())
                            .forEach(emailList::add);
                }

                emailLog.setTotalApplicable(emailList.size());

                String logoBase64 = getApartmentLogo(notice.getAprmtId());
                String htmlBody = buildNoticeEmailHtml(logoBase64, notice);
                String subject = "Notice: " + notice.getLetterNumber();

                for (String email : emailList) {
                    sendHtmlEmail(email, subject, htmlBody);
                }
                if (!emailList.isEmpty()) {
                    emailSentCount++;
                }

                notice.setEmailSentflag(EMAIL_SENT_FLAG_YES);
                noticeRepository.save(notice);

            } catch (Exception e) {
                logger.error("Failed to send notice email for {}: {}", notice.getNoticeId(), e.getMessage(), e);
                FailedEmailCause failedCause = new FailedEmailCause(
                        notice.getNoticeId(), EMAIL_LOG_TYPE_NOTICE, e.getMessage(), notice.getNoticeId()
                );
                failedCauses.add(failedCause);
            }

            emailLog.setEmailSent(emailSentCount);
            emailLog.setFailedEmailCause(toJson(failedCauses));
            emailLogRepository.save(emailLog);
        }
    }

    // -------------------------------------------------------------------------
    // Retry helpers
    // -------------------------------------------------------------------------

    private void retryPaymentEmailForFlats(PaymentEntity payment, List<String> failedFlats, SecuraEmailLog log) {
        List<String> stillFailed = new ArrayList<>();
        List<FailedEmailCause> failedCauses = new ArrayList<>();
        int sentCount = log.getEmailSent() != null ? log.getEmailSent() : 0;

        for (String flatId : failedFlats) {
            try {
                Optional<Flat> optFlat = flatRepository.findById(flatId);
                if (optFlat.isEmpty()) continue;
                Flat flat = optFlat.get();

                List<Owner> owners = ownerRepository.findByFlatNo(flatId);
                List<Profile> profiles = owners.stream()
                        .map(o -> profileRepository.findById(o.getPrflId()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                List<String> emailList = profiles.stream()
                        .map(Profile::getPrflEmailAdrss)
                        .filter(e -> e != null && !e.isBlank())
                        .collect(Collectors.toList());

                if (emailList.isEmpty()) continue;

                String ownerName = buildOwnerNames(profiles);

                GetDueAmountForFlatRequest dueRequest = new GetDueAmountForFlatRequest();
                GenericHeader header = new GenericHeader();
                header.setApartmentId(payment.getAprmtId());
                dueRequest.setGenericHeader(header);
                dueRequest.setFlatId(flatId);
                GetDueAmountForFlatResponse dueResponse = flatServices.getDueAmountForFlat(dueRequest);

                List<DueAmountDetailsEntity> currentPaymentDues = extractDuesForPayment(dueResponse, payment.getPaymentId());

                List<DueAmountDetailsEntity> allDues = dueAmountDetailsRepository.findByPaymentId(payment.getPaymentId());
                boolean isPerSqft = "PER_SQFT".equalsIgnoreCase(payment.getPaymentCapita());
                List<DueAmountDetailsEntity> flatDues = allDues.stream()
                        .filter(d -> isPerSqft ? flat.getFlatArea() != null && flat.getFlatArea().equals(d.getFlatArea()) : true)
                        .collect(Collectors.toList());

                List<DueAmountDetailsEntity> highestCycleDues = findHighestCycleDues(flatDues);
                double paymentTotalDue = highestCycleDues.stream().mapToDouble(d -> parseDouble(d.getTotalAmount())).sum();

                List<String> paidFlatList = parseStringList(payment.getPaidFlats());
                List<DueAmountDetailsEntity> upcomingDues = highestCycleDues.stream()
                        .filter(d -> !paidFlatList.contains(flatId))
                        .collect(Collectors.toList());
                List<DueAmountDetailsEntity> upcomingDuesByDate = getUpcomingDuesSortedByDate(upcomingDues);

                List<String> discFinCodes = parseStringList(payment.getDiscFin());
                List<DiscFin> discFinList = new ArrayList<>();
                for (String code : discFinCodes) {
                    discFinRepository.findByDiscFnId(code).stream()
                            .filter(d -> "ACTIVE".equalsIgnoreCase(d.getDiscFnType()))
                            .forEach(discFinList::add);
                }

                String logoBase64 = getApartmentLogo(payment.getAprmtId());
                String htmlBody = buildPaymentEmailHtml(
                        ownerName, logoBase64,
                        payment.getPaymentName(), payment.getShortDetails(), payment.getCauseId(),
                        payment.getCollectionStartDate() != null ? payment.getCollectionStartDate().format(DATE_FMT) : "-",
                        payment.getCollectionEndDate() != null ? payment.getCollectionEndDate().format(DATE_FMT) : "-",
                        payment.getAllowedPaymentModes(), payment.getPaymentAmount(),
                        isPerSqft, payment.getGst(), payment.getPaymentType(),
                        discFinList, upcomingDuesByDate, paymentTotalDue, currentPaymentDues
                );

                String subject = "Due Created For " + payment.getPaymentName();
                for (String email : emailList) {
                    sendHtmlEmail(email, subject, htmlBody);
                }
                sentCount++;

            } catch (Exception e) {
                logger.error("Retry failed for flat {}: {}", flatId, e.getMessage(), e);
                stillFailed.add(flatId);
                failedCauses.add(new FailedEmailCause(payment.getPaymentId(), EMAIL_LOG_TYPE_PAYMENT, e.getMessage(), flatId));
            }
        }

        log.setAttempt(log.getAttempt() != null ? log.getAttempt() + 1 : 2);
        log.setEmailSent(sentCount);
        log.setFailedApplicableList(toJson(stillFailed));
        log.setFailedEmailCause(toJson(failedCauses));
        emailLogRepository.save(log);
    }

    private void retryTransactionEmail(SecuraEmailLog log) {
        // Re-attempt is a no-op placeholder; transaction retry mirrors the initial send logic
        log.setAttempt(log.getAttempt() != null ? log.getAttempt() + 1 : 2);
        emailLogRepository.save(log);
    }

    private void retryNoticeEmail(SecuraEmailLog log) {
        // Re-attempt is a no-op placeholder; notice retry mirrors the initial send logic
        log.setAttempt(log.getAttempt() != null ? log.getAttempt() + 1 : 2);
        emailLogRepository.save(log);
    }

    // -------------------------------------------------------------------------
    // HTML Email Builders
    // -------------------------------------------------------------------------

    private String buildPaymentEmailHtml(
            String ownerName,
            String logoBase64,
            String paymentName, String shortDesc, String cause,
            String startDate, String endDate,
            String allowedTenders, String unitAmount, boolean isPerSqft,
            String gst, String paymentType,
            List<DiscFin> discFinList,
            List<DueAmountDetailsEntity> upcomingDues,
            double paymentTotalDue,
            List<DueAmountDetailsEntity> currentPaymentDues) {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>")
            .append("<meta charset='UTF-8'>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;color:#333;margin:0;padding:0;background:#f4f4f4;}")
            .append(".container{max-width:640px;margin:20px auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);}")
            .append(".header{background:#1a237e;padding:20px;text-align:center;}")
            .append(".header img{max-height:60px;}")
            .append(".header h1{color:#fff;margin:10px 0 0;font-size:20px;}")
            .append(".section{padding:20px 30px;border-bottom:1px solid #eee;}")
            .append(".section h2{font-size:15px;color:#1a237e;margin-bottom:10px;border-bottom:2px solid #1a237e;padding-bottom:4px;}")
            .append(".row{display:flex;justify-content:space-between;padding:4px 0;font-size:14px;}")
            .append(".label{font-weight:bold;color:#555;min-width:180px;}")
            .append(".value{color:#333;}")
            .append(".due-table{width:100%;border-collapse:collapse;font-size:13px;margin-top:8px;}")
            .append(".due-table th{background:#1a237e;color:#fff;padding:8px;text-align:left;}")
            .append(".due-table td{padding:7px 8px;border-bottom:1px solid #eee;}")
            .append(".due-table tr:nth-child(even) td{background:#f9f9f9;}")
            .append(".footer{background:#f4f4f4;padding:12px 30px;text-align:center;font-size:12px;color:#888;}")
            .append("</style></head><body>")
            .append("<div class='container'>")
            .append("<div class='header'>");

        // Logo
        if (logoBase64 != null && !logoBase64.isBlank()) {
            html.append("<img src='").append(logoBase64).append("' alt='Apartment Logo'/>");
        }
        html.append("<h1>Due Created For ").append(escapeHtml(paymentName)).append("</h1>")
            .append("</div>");

        // Greeting
        html.append("<div class='section'>")
            .append("<p>Dear <strong>").append(escapeHtml(ownerName)).append("</strong>,</p>")
            .append("<p>We would like to inform you about the following payment due details.</p>")
            .append("</div>");

        // Payment Details section
        html.append("<div class='section'><h2>Payment Details</h2>");
        appendRow(html, "Payment Name", paymentName);
        appendRow(html, "Description", shortDesc);
        appendRow(html, "Cause", cause);
        appendRow(html, "Payment Tenure", startDate + " to " + endDate);
        appendRow(html, "Allowed Tenders", allowedTenders);
        String amountDisplay = unitAmount + (isPerSqft ? " Per Sqft" : "");
        appendRow(html, "Amount", amountDisplay);
        appendRow(html, "GST", gst != null ? gst + "%" : "-");
        appendRow(html, "Payment Type", paymentType);
        html.append("</div>");

        // Discount Details section
        if (!discFinList.isEmpty()) {
            html.append("<div class='section'><h2>Discount Details</h2>");
            // Group by discFnId
            Map<String, List<DiscFin>> grouped = discFinList.stream()
                    .collect(Collectors.groupingBy(DiscFin::getDiscFnId));
            for (Map.Entry<String, List<DiscFin>> entry : grouped.entrySet()) {
                List<DiscFin> group = entry.getValue();
                DiscFin first = group.get(0);
                String discountType = first.getDiscFnType();
                String discFrom = first.getDiscFnStrtDt() != null ? first.getDiscFnStrtDt().format(DATE_FMT) : "-";
                String discTo   = first.getDiscFnEndDt()  != null ? first.getDiscFnEndDt().format(DATE_FMT)  : "-";
                String discMode = first.getDiscFnMode() != null ? first.getDiscFnMode().replace("_", " ") : "-";
                String discValue = first.getDiscFinValue();
                boolean isPercentage = "PERCENTAGE".equalsIgnoreCase(first.getDiscFnMode());
                String discValueDisplay = discValue != null ? (isPercentage ? discValue + "%" : discValue) : "-";

                // Applicable For
                String applicableFor;
                if (group.size() > 1) {
                    applicableFor = group.stream()
                            .map(DiscFin::getDiscFnCycleType)
                            .filter(v -> v != null)
                            .collect(Collectors.joining(", "));
                } else {
                    String cycleType = first.getDiscFnCycleType();
                    if ("FIXED".equalsIgnoreCase(cycleType)) {
                        applicableFor = "FLAT";
                    } else {
                        applicableFor = cycleType != null ? cycleType.replace("_", " ") : "-";
                    }
                }

                appendRow(html, "Discount Type", discountType);
                appendRow(html, "From &amp; To", discFrom + " - " + discTo);
                appendRow(html, "Applicable For", applicableFor);
                appendRow(html, "Discount Mode", discMode);
                appendRow(html, "Value", discValueDisplay);
                html.append("<hr style='border:none;border-top:1px dashed #ccc;margin:8px 0;'/>");
            }
            html.append("</div>");
        }

        // Upcoming Due section
        if (!upcomingDues.isEmpty()) {
            html.append("<div class='section'><h2>Due Details</h2>")
                .append("<p><strong>Upcoming Due</strong></p>")
                .append("<table class='due-table'><thead><tr>")
                .append("<th>Cycle</th><th>Due Date</th><th>Amount</th>")
                .append("</tr></thead><tbody>");

            for (DueAmountDetailsEntity due : upcomingDues) {
                html.append("<tr>")
                    .append("<td>").append(escapeHtml(due.getCollectionCycle())).append("</td>")
                    .append("<td>").append(due.getDueDate() != null ? due.getDueDate().format(DATE_FMT) : "-").append("</td>")
                    .append("<td>").append(escapeHtml(due.getTotalAmount())).append("</td>")
                    .append("</tr>");
            }
            html.append("</tbody></table>");
            html.append("</div>");
        }

        html.append("<div class='footer'>")
            .append("This is an automated notification. Please do not reply to this email.")
            .append("</div>")
            .append("</div></body></html>");

        return html.toString();
    }

    private String buildTransactionEmailHtml(String ownerName, String logoBase64, Transaction transaction) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<style>body{font-family:Arial,sans-serif;color:#333;background:#f4f4f4;margin:0;padding:0;}")
            .append(".container{max-width:600px;margin:20px auto;background:#fff;border-radius:8px;padding:20px;box-shadow:0 2px 8px rgba(0,0,0,0.1);}")
            .append(".header{background:#1a237e;padding:16px;border-radius:6px 6px 0 0;text-align:center;color:#fff;}")
            .append(".section{padding:16px 0;border-bottom:1px solid #eee;font-size:14px;}")
            .append(".label{font-weight:bold;color:#555;display:inline-block;min-width:160px;}")
            .append(".footer{text-align:center;font-size:12px;color:#888;padding-top:12px;}")
            .append("</style></head><body><div class='container'>")
            .append("<div class='header'>");

        if (logoBase64 != null && !logoBase64.isBlank()) {
            html.append("<img src='").append(logoBase64).append("' alt='Logo' style='max-height:50px;'/><br/>");
        }
        html.append("<h2 style='margin:8px 0 0;'>Transaction Confirmation</h2></div>")
            .append("<div class='section'>Dear <strong>").append(escapeHtml(ownerName)).append("</strong>,</div>")
            .append("<div class='section'>");
        appendRowInline(html, "Transaction ID", transaction.getTrnscId());
        appendRowInline(html, "Date", transaction.getTrnsDate() != null ? transaction.getTrnsDate().format(DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm")) : "-");
        appendRowInline(html, "Amount", transaction.getTrnsAmt());
        appendRowInline(html, "Type", transaction.getTrnsType());
        appendRowInline(html, "Status", transaction.getTrnsStatus());
        appendRowInline(html, "Description", transaction.getTrnsShrtDesc());
        html.append("</div>")
            .append("<div class='footer'>This is an automated notification. Please do not reply to this email.</div>")
            .append("</div></body></html>");
        return html.toString();
    }

    private String buildNoticeEmailHtml(String logoBase64, NoticeEntity notice) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<style>body{font-family:Arial,sans-serif;color:#333;background:#f4f4f4;margin:0;padding:0;}")
            .append(".container{max-width:600px;margin:20px auto;background:#fff;border-radius:8px;padding:20px;box-shadow:0 2px 8px rgba(0,0,0,0.1);}")
            .append(".header{background:#1a237e;padding:16px;border-radius:6px 6px 0 0;text-align:center;color:#fff;}")
            .append(".body-section{padding:16px 0;font-size:14px;}")
            .append(".footer{text-align:center;font-size:12px;color:#888;padding-top:12px;border-top:1px solid #eee;}")
            .append("</style></head><body><div class='container'>")
            .append("<div class='header'>");

        if (logoBase64 != null && !logoBase64.isBlank()) {
            html.append("<img src='").append(logoBase64).append("' alt='Logo' style='max-height:50px;'/><br/>");
        }
        html.append("<h2 style='margin:8px 0 0;'>Notice: ").append(escapeHtml(notice.getLetterNumber())).append("</h2></div>")
            .append("<div class='body-section'>")
            .append("<h3>").append(escapeHtml(notice.getNoticeHeader())).append("</h3>")
            .append("<p>").append(escapeHtml(notice.getShortDetails())).append("</p>")
            .append("</div>")
            .append("<div class='footer'>This is an automated notification. Please do not reply to this email.</div>")
            .append("</div></body></html>");
        return html.toString();
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private SecuraEmailLog createInitialEmailLog(String type, String referenceUniqueId) {
        SecuraEmailLog log = new SecuraEmailLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setType(type);
        log.setReferenceUniqueId(referenceUniqueId);
        log.setAttempt(1);
        log.setTotalApplicable(0);
        log.setEmailSent(0);
        return log;
    }

    private List<String> getApplicableFlats(PaymentEntity payment) {
        String applicableFor = payment.getApplicableFor();
        if (applicableFor == null || applicableFor.isBlank()) {
            return new ArrayList<>();
        }
        if ("ALL".equalsIgnoreCase(applicableFor.trim())) {
            return flatRepository.findByAprmntId(payment.getAprmtId()).stream()
                    .map(Flat::getFlatNo)
                    .collect(Collectors.toList());
        }
        return parseStringList(applicableFor);
    }

    private List<DueAmountDetailsEntity> extractDuesForPayment(GetDueAmountForFlatResponse dueResponse, String paymentId) {
        if (dueResponse == null || dueResponse.getDueDetails() == null) {
            return new ArrayList<>();
        }
        return dueResponse.getDueDetails().entrySet().stream()
                .filter(e -> paymentId.equals(e.getKey().getPaymentId()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());
    }

    private List<DueAmountDetailsEntity> findHighestCycleDues(List<DueAmountDetailsEntity> dues) {
        for (String cycle : CYCLE_PRIORITY) {
            List<DueAmountDetailsEntity> filtered = dues.stream()
                    .filter(d -> cycle.equalsIgnoreCase(d.getCollectionCycle()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                return filtered;
            }
        }
        return dues;
    }

    private List<DueAmountDetailsEntity> getUpcomingDuesSortedByDate(List<DueAmountDetailsEntity> dues) {
        LocalDate today = LocalDate.now();
        return dues.stream()
                .filter(d -> d.getDueDate() != null && !d.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(DueAmountDetailsEntity::getDueDate))
                .collect(Collectors.toList());
    }

    private String buildOwnerNames(List<Profile> profiles) {
        if (profiles.isEmpty()) return "Resident";
        return profiles.stream()
                .map(p -> p.getPrflName() != null ? genericService.fromJson(p.getPrflName(), Name.class).toString() : "")
                .filter(n -> !n.isBlank())
                .collect(Collectors.joining(" & "));
    }

    private String getApartmentLogo(String aprmtId) {
        if (aprmtId == null) return null;
        Optional<ApartmentMaster> opt = apartmentRepository.findById(aprmtId);
        return opt.map(ApartmentMaster::getAprmnt_logo).orElse(null);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
        sendHtmlEmailWithLogo(to, subject, htmlBody, null);
    }

    private void sendHtmlEmailWithLogo(String to, String subject, String htmlBody, String logoBase64) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (logoBase64 != null && !logoBase64.isEmpty()) {
                byte[] logoBytes = java.util.Base64.getDecoder().decode(logoBase64);
                String mimeType = detectImageMimeType(logoBase64);
                org.springframework.core.io.ByteArrayResource logoResource =
                        new org.springframework.core.io.ByteArrayResource(logoBytes);
                helper.addInline("societylogo", logoResource, mimeType);
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send email to [" + to + "] with subject [" + subject + "]: " + e.getMessage(), e);
        }
    }
    
    private void sendHtmlEmailWithLogoAndAttachment(
            String to,
            String subject,
            String htmlBody,
            String logoBase64,
            byte[] attachmentBytes,
            String attachmentName) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (logoBase64 != null && !logoBase64.isEmpty()) {
                byte[] logoBytes = Base64.getDecoder().decode(logoBase64);
                String mimeType = detectImageMimeType(logoBase64);
                org.springframework.core.io.ByteArrayResource logoResource =
                        new org.springframework.core.io.ByteArrayResource(logoBytes);
                helper.addInline("societylogo", logoResource, mimeType);
            }
            if (attachmentBytes != null && attachmentBytes.length > 0) {
                org.springframework.core.io.ByteArrayResource attachmentResource =
                        new org.springframework.core.io.ByteArrayResource(attachmentBytes);
                helper.addAttachment(attachmentName, attachmentResource, "application/pdf");
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send email to [" + to + "] with subject [" + subject + "]: " + e.getMessage(), e);
        }
    }

    private String detectImageMimeType(String base64) {
        if (base64 == null || base64.length() < 8) return "image/png";
        String prefix = base64.substring(0, Math.min(base64.length(), 12));
        if (prefix.startsWith("/9j/")) return "image/jpeg";
        if (prefix.startsWith("R0lGOD"))  return "image/gif";
        if (prefix.startsWith("UklGR"))   return "image/webp";
        return "image/png";
    }

    private byte[] getReceiptPdfBytes(Transaction transaction) {
        if (transaction == null || transaction.getReceiptNumber() == null || transaction.getReceiptNumber().isBlank()) {
            return null;
        }
        List<Receipt> receipts = receiptRepository.findByAprmtIdAndReceiptId(transaction.getAprmntId(), transaction.getReceiptNumber());
        if (receipts.isEmpty()) {
            receipts = receiptRepository.findByReceiptId(transaction.getReceiptNumber());
        }
        for (Receipt receipt : receipts) {
            String receiptBase64 = extractReceiptBase64(receipt != null ? receipt.getReceiptData() : null);
            if (receiptBase64 == null || receiptBase64.isBlank()) {
                continue;
            }
            try {
                return Base64.getDecoder().decode(receiptBase64);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid base64 receipt content for receipt {}", transaction.getReceiptNumber());
            }
        }
        return null;
    }

    private String extractReceiptBase64(String receiptData) {
        if (receiptData == null || receiptData.isBlank()) {
            return null;
        }
        String trimmedData = receiptData.trim();
        String prefix = "data:application/pdf;base64,";
        if (trimmedData.startsWith(prefix)) {
            return trimmedData.substring(prefix.length());
        }
        if (!trimmedData.startsWith("{")) {
            return trimmedData;
        }
        try {
            var node = OBJECT_MAPPER.readTree(trimmedData);
            if (node.hasNonNull("receipt")) {
                return node.get("receipt").asText();
            }
            if (node.hasNonNull("pdfReceipt")) {
                return node.get("pdfReceipt").asText();
            }
            if (node.hasNonNull("receiptData")) {
                return node.get("receiptData").asText();
            }
        } catch (Exception e) {
            logger.warn("Unable to parse receipt data for attachment", e);
        }
        return null;
    }

    private String getReceiptAttachmentName(Transaction transaction) {
        String receiptNumber = transaction != null ? transaction.getReceiptNumber() : null;
        if (receiptNumber == null || receiptNumber.isBlank()) {
            return "transaction-receipt.pdf";
        }
        return receiptNumber + ".pdf";
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        sb.append("<div class='row'>")
          .append("<span class='label'>").append(escapeHtml(label)).append(":</span>")
          .append("<span class='value'>").append(escapeHtml(value != null ? value : "-")).append("</span>")
          .append("</div>");
    }

    private void appendRowInline(StringBuilder sb, String label, String value) {
        sb.append("<p><span class='label'>").append(escapeHtml(label)).append(": </span>")
          .append(escapeHtml(value != null ? value : "-")).append("</p>");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        json = json.trim();
        try {
            return genericService.fromJson(json, new TypeReference<List<String>>() {
			});// OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Could be a single value
            return new ArrayList<>(List.of(json));
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
