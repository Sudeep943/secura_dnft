package com.secura.dnft.generic.bean;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PaymentCauseCode {

MAINTENANCE("MAINTENANCE", "MANT"),
FESTIVAL_FUND("FESTIVAL_FUND", "EVNT"),
SECURITY_CHARGES("SECURITY_CHARGES", "SCCHRGS"),
CORPUS_FUND("CORPUS_FUND", "CRPSFND"),
CLUBHOUSE_CHARGES("CLUBHOUSE_CHARGES", "CLBCHRGS"),
CAR_PARKING_CHARGES("CAR_PARKING_CHARGES", "PRKCHRGS"),
POWER_BACKUP_CHARGES("POWER_BACKUP_CHARGES", "PWRBKP"),
WATER_CHARGES_SOCIETY_COLLECTED("WATER_CHARGES_SOCIETY_COLLECTED", "WTRCHRGS");

private static final String DEFAULT_CODE = "OTHR";
private static final Map<String, PaymentCauseCode> LOOKUP = Arrays.stream(values())
.collect(Collectors.toMap(PaymentCauseCode::getCauseName, Function.identity()));

private final String causeName;
private final String code;

PaymentCauseCode(String causeName, String code) {
this.causeName = causeName;
this.code = code;
}

public String getCauseName() {
return causeName;
}

public String getCode() {
return code;
}

public static String getCode(String cause) {
String normalized = normalizeCause(cause);
if (normalized == null || normalized.isEmpty()) {
return DEFAULT_CODE;
}
PaymentCauseCode paymentCauseCode = LOOKUP.get(normalized);
return paymentCauseCode != null ? paymentCauseCode.code : DEFAULT_CODE;
}

private static String normalizeCause(String cause) {
if (cause == null) {
return null;
}
return cause.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
}
}
