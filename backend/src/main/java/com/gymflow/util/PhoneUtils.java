package com.gymflow.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    /**
     * Normalizes a phone number to Indian format (+91XXXXXXXXXX).
     * If the phone number is null or empty, it returns null.
     */
    public static String normalizeToIndianPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        // Strip everything except digits and '+'
        String cleaned = phone.replaceAll("[^0-9+]", "");
        
        if (cleaned.isEmpty() || cleaned.equals("+")) {
            return null;
        }

        // If it starts with +91 or 91 and has at least 10 remaining digits
        if (cleaned.startsWith("+91") && cleaned.length() >= 13) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() >= 12) {
            cleaned = cleaned.substring(2);
        }
        
        // Strip any remaining plus signs
        cleaned = cleaned.replace("+", "");
        
        if (cleaned.isEmpty()) {
            return null;
        }

        return "+91" + cleaned;
    }
}
