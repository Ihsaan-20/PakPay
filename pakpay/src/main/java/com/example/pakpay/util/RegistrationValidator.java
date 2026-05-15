package com.example.pakpay.util;

import java.util.regex.Pattern;

public final class RegistrationValidator {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^03\\d{9}$");
    private static final Pattern CNIC_PATTERN = Pattern.compile("^\\d{13}$");

    private RegistrationValidator() {}

    public static String normalizeMobile(String mobile) {
        if (mobile == null) {
            return "";
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.startsWith("92") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    public static String normalizeCnic(String cnic) {
        if (cnic == null) {
            return "";
        }
        return cnic.replaceAll("\\D", "");
    }

    public static boolean isValidMobile(String mobile) {
        return MOBILE_PATTERN.matcher(normalizeMobile(mobile)).matches();
    }

    public static boolean isValidCnic(String cnic) {
        return CNIC_PATTERN.matcher(normalizeCnic(cnic)).matches();
    }

    public static String buildVirtualEmail(String normalizedMobile) {
        return normalizedMobile + "@pakpay.com";
    }
}
