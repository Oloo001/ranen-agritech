package com.ranen.ranen.common;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PhoneNumbers {

    // Kenyan mobile numbers: +254 then 7xx or 1xx, then 7 more digits.
    private static final Pattern KENYA_MOBILE = Pattern.compile("^\\+254[17]\\d{8}$");

    private PhoneNumbers() {
    }

    /** Returns the number as +254XXXXXXXXX, or empty if it isn't a valid Kenyan mobile number. */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.replaceAll("[\\s\\-()]", "");
        if (s.startsWith("254")) {
            s = "+" + s;
        } else if (s.startsWith("0")) {
            s = "+254" + s.substring(1);
        } else if (!s.startsWith("+") && s.length() == 9) {
            s = "+254" + s;
        }
        return KENYA_MOBILE.matcher(s).matches() ? Optional.of(s) : Optional.empty();
    }
}
