package com.carddemo.seed;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Reads one fixed-width COBOL record field by field, in copybook order.
 * Each method consumes the next field, so the calling code reads like the copybook itself.
 */
public final class CopybookReader {

    private final String record;
    private int position;

    public CopybookReader(String record) {
        this.record = record;
    }

    /** PIC X(n): space-padded text. Trailing pad spaces are removed. */
    public String text(int length) {
        return take(length).stripTrailing();
    }

    /** PIC 9(n): unsigned display digits. */
    public long unsignedLong(int digits) {
        return Long.parseLong(take(digits));
    }

    /** PIC 9(n): unsigned display digits. */
    public int unsignedInt(int digits) {
        return Integer.parseInt(take(digits));
    }

    /** PIC 9(n) where leading zeros matter (e.g. SSN), kept as text. */
    public String digits(int digits) {
        return take(digits);
    }

    /**
     * PIC S9(i)V9(d): signed zoned decimal with an implied decimal point.
     * The sign is "overpunched" into the last character: '{' and 'A'-'I' mean +0..+9,
     * '}' and 'J'-'R' mean -0..-9.
     */
    public BigDecimal signedDecimal(int integerDigits, int decimalDigits) {
        String raw = take(integerDigits + decimalDigits);
        char last = raw.charAt(raw.length() - 1);
        int lastDigit;
        boolean negative;
        if (last >= '0' && last <= '9') {
            lastDigit = last - '0';
            negative = false;
        } else if (last == '{') {
            lastDigit = 0;
            negative = false;
        } else if (last >= 'A' && last <= 'I') {
            lastDigit = last - 'A' + 1;
            negative = false;
        } else if (last == '}') {
            lastDigit = 0;
            negative = true;
        } else if (last >= 'J' && last <= 'R') {
            lastDigit = last - 'J' + 1;
            negative = true;
        } else {
            throw new IllegalArgumentException("Invalid zoned-decimal sign character '" + last + "' in '" + raw + "'");
        }
        BigInteger unscaled = new BigInteger(raw.substring(0, raw.length() - 1) + lastDigit);
        return new BigDecimal(negative ? unscaled.negate() : unscaled, decimalDigits);
    }

    /** PIC X(10) holding a YYYY-MM-DD date. */
    public LocalDate date() {
        return LocalDate.parse(take(10));
    }

    /** FILLER: unused bytes. */
    public void skip(int length) {
        take(length);
    }

    private String take(int length) {
        int end = position + length;
        String value;
        if (end <= record.length()) {
            value = record.substring(position, end);
        } else {
            // Sample files sometimes omit trailing FILLER spaces; treat missing bytes as spaces.
            String available = position < record.length() ? record.substring(position) : "";
            value = available + " ".repeat(length - available.length());
        }
        position = end;
        return value;
    }
}
