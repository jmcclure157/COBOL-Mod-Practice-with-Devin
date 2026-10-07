package com.carddemo.batch;

import java.util.Locale;

/** Builds CVTRA06Y DALYTRAN-RECORD lines (350 bytes) for tests. */
final class DailyRecords {

    private DailyRecords() {
    }

    /** {@code amountInCents} is written as PIC S9(09)V99 zoned decimal, sign overpunched on the last digit. */
    static String line(String id, String cardNumber, long amountInCents, String originTimestamp) {
        String digits = String.format(Locale.ROOT, "%011d", Math.abs(amountInCents));
        int last = digits.charAt(10) - '0';
        char sign = amountInCents < 0 ? "}JKLMNOPQR".charAt(last) : "{ABCDEFGHI".charAt(last);
        String line = String.format(Locale.ROOT, "%-16s%-2s%04d%-10s%-100s%s%09d%-50s%-50s%-10s%-16s%-26s%-26s%-20s",
                id, "01", 1, "POS TERM", "Test purchase", digits.substring(0, 10) + sign, 123456789L,
                "Test Merchant", "Test City", "12345", cardNumber, originTimestamp, "", "");
        if (line.length() != 350) {
            throw new IllegalStateException("record is " + line.length() + " bytes");
        }
        return line;
    }
}
