package com.sociolab.surehealth.logging;

public class LogUtil {

    private LogUtil() {} // prevent instantiation

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        String[] parts = email.split("@");
        String namePart = parts[0];
        String domainPart = parts[1];
        String maskedName = namePart.length() <= 2 ? "**" : namePart.substring(0, 2) + "*".repeat(namePart.length() - 2);
        return maskedName + "@" + domainPart;
    }
}
