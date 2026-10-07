package com.carddemo.service;

import com.carddemo.model.CardXref;
import com.carddemo.model.Transaction;
import com.carddemo.repository.CardXrefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Business logic of CICS program COTRN02C (transaction CT02, "Add Transaction"), without the screen handling.
 * Edits run in the COBOL order and stop at the first failure, like each SEND-TRNADD-SCREEN + RETURN did.
 */
@Service
public class TransactionAddService {

    static final String MSG_ACCOUNT_NOT_NUMERIC = "Account ID must be Numeric...";
    static final String MSG_CARD_NOT_NUMERIC = "Card Number must be Numeric...";
    static final String MSG_KEY_MISSING = "Account or Card Number must be entered...";
    static final String MSG_ACCOUNT_NOT_FOUND = "Account ID NOT found...";
    static final String MSG_CARD_NOT_FOUND = "Card Number NOT found...";
    static final String MSG_TYPE_NOT_NUMERIC = "Type CD must be Numeric...";
    static final String MSG_CATEGORY_NOT_NUMERIC = "Category CD must be Numeric...";
    static final String MSG_AMOUNT_FORMAT = "Amount should be in format -99999999.99";
    static final String MSG_ORIG_DATE_FORMAT = "Orig Date should be in format YYYY-MM-DD";
    static final String MSG_PROC_DATE_FORMAT = "Proc Date should be in format YYYY-MM-DD";
    static final String MSG_ORIG_DATE_INVALID = "Orig Date - Not a valid date...";
    static final String MSG_PROC_DATE_INVALID = "Proc Date - Not a valid date...";
    static final String MSG_MERCHANT_ID_NOT_NUMERIC = "Merchant ID must be Numeric...";
    static final String MSG_CONFIRM = "Confirm to add this transaction...";
    static final String MSG_CONFIRM_INVALID = "Invalid value. Valid values are (Y/N)...";
    static final String MSG_UNABLE_TO_ADD = "Unable to Add Transaction...";

    private static final Pattern AMOUNT = Pattern.compile("[+-]\\d{8}\\.\\d{2}");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final CardXrefRepository cardXrefs;
    private final TransactionWriter writer;

    public TransactionAddService(CardXrefRepository cardXrefs, TransactionWriter writer) {
        this.cardXrefs = cardXrefs;
        this.writer = writer;
    }

    /** PROCESS-ENTER-KEY: key fields, then data fields, then the Y/N confirmation, then ADD-TRANSACTION. */
    @Transactional
    public NewTransactionResult addTransaction(NewTransactionRequest in) {
        String cardNumber = validateKeyFields(in.accountId(), in.cardNumber());
        Transaction t = validateDataFields(in);
        validateConfirm(in.confirm());

        t.setCardNumber(cardNumber);
        String id = writer.insertWithNextId(t, MSG_UNABLE_TO_ADD);
        return new NewTransactionResult(id, "Transaction added successfully.  Your Tran ID is " + id + ".");
    }

    /**
     * VALIDATE-INPUT-KEY-FIELDS: an account id wins over a card number. The account is looked up in CXACAIX to get
     * its card; a card is looked up in CCXREF. Like the account view, shorter numbers are accepted and zero-padded.
     */
    private String validateKeyFields(String rawAccountId, String rawCardNumber) {
        String accountId = text(rawAccountId);
        String cardNumber = text(rawCardNumber);
        if (!accountId.isEmpty()) {
            if (accountId.length() > 11 || !isDigits(accountId)) {
                throw new InvalidNewTransactionException(MSG_ACCOUNT_NOT_NUMERIC);
            }
            return cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(Long.parseLong(accountId))
                    .map(CardXref::getCardNumber)
                    .orElseThrow(() -> new CardOrAccountNotFoundException(MSG_ACCOUNT_NOT_FOUND));
        }
        if (!cardNumber.isEmpty()) {
            if (cardNumber.length() > 16 || !isDigits(cardNumber)) {
                throw new InvalidNewTransactionException(MSG_CARD_NOT_NUMERIC);
            }
            return cardXrefs.findById(zeroPad(cardNumber, 16))
                    .map(CardXref::getCardNumber)
                    .orElseThrow(() -> new CardOrAccountNotFoundException(MSG_CARD_NOT_FOUND));
        }
        throw new InvalidNewTransactionException(MSG_KEY_MISSING);
    }

    /** VALIDATE-INPUT-DATA-FIELDS, in the COBOL order: required fields, numeric codes, formats, real dates. */
    private Transaction validateDataFields(NewTransactionRequest in) {
        String typeCode = required(in.typeCode(), "Type CD").strip();
        String categoryCode = required(in.categoryCode(), "Category CD").strip();
        String source = required(in.source(), "Source");
        String description = required(in.description(), "Description");
        String amount = required(in.amount(), "Amount").strip();
        String originDate = required(in.originDate(), "Orig Date").strip();
        String processedDate = required(in.processedDate(), "Proc Date").strip();
        String merchantId = required(in.merchantId(), "Merchant ID").strip();
        String merchantName = required(in.merchantName(), "Merchant Name");
        String merchantCity = required(in.merchantCity(), "Merchant City");
        String merchantZip = required(in.merchantZip(), "Merchant Zip");

        if (typeCode.length() > 2 || !isDigits(typeCode)) {
            throw new InvalidNewTransactionException(MSG_TYPE_NOT_NUMERIC);
        }
        if (categoryCode.length() > 4 || !isDigits(categoryCode)) {
            throw new InvalidNewTransactionException(MSG_CATEGORY_NOT_NUMERIC);
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidNewTransactionException(MSG_AMOUNT_FORMAT);
        }
        if (!DATE.matcher(originDate).matches()) {
            throw new InvalidNewTransactionException(MSG_ORIG_DATE_FORMAT);
        }
        if (!DATE.matcher(processedDate).matches()) {
            throw new InvalidNewTransactionException(MSG_PROC_DATE_FORMAT);
        }
        requireRealDate(originDate, MSG_ORIG_DATE_INVALID);
        requireRealDate(processedDate, MSG_PROC_DATE_INVALID);
        if (merchantId.length() > 9 || !isDigits(merchantId)) {
            throw new InvalidNewTransactionException(MSG_MERCHANT_ID_NOT_NUMERIC);
        }
        // Not in COBOL (the 3270 fields could not be overfilled): reject values wider than the CVTRA05Y fields.
        maxLength(source, 10, "Source");
        maxLength(description, 100, "Description");
        maxLength(merchantName, 50, "Merchant Name");
        maxLength(merchantCity, 50, "Merchant City");
        maxLength(merchantZip, 10, "Merchant Zip");

        var t = new Transaction();
        t.setTypeCode(zeroPad(typeCode, 2));
        t.setCategoryCode(Integer.parseInt(categoryCode));
        t.setSource(source);
        t.setDescription(description);
        t.setAmount(new BigDecimal(amount));
        t.setOriginTimestamp(originDate);
        t.setProcessedTimestamp(processedDate);
        t.setMerchantId(Long.parseLong(merchantId));
        t.setMerchantName(merchantName);
        t.setMerchantCity(merchantCity);
        t.setMerchantZip(merchantZip);
        return t;
    }

    /** The CONFIRM field: Y adds, N or blank asks to confirm, anything else is invalid. */
    private static void validateConfirm(String rawConfirm) {
        String confirm = text(rawConfirm);
        switch (confirm) {
            case "Y", "y" -> { }
            case "N", "n", "" -> throw new InvalidNewTransactionException(MSG_CONFIRM);
            default -> throw new InvalidNewTransactionException(MSG_CONFIRM_INVALID);
        }
    }

    private static String required(String raw, String label) {
        String value = raw == null ? "" : raw.stripTrailing();
        if (value.isBlank()) {
            throw new InvalidNewTransactionException(label + " can NOT be empty...");
        }
        return value;
    }

    private static void requireRealDate(String date, String message) {
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidNewTransactionException(message);
        }
    }

    private static void maxLength(String value, int max, String label) {
        if (value.length() > max) {
            throw new InvalidNewTransactionException(label + " must be at most " + max + " characters");
        }
    }

    private static String text(String raw) {
        return raw == null ? "" : raw.strip();
    }

    private static boolean isDigits(String value) {
        return value.chars().allMatch(ch -> ch >= '0' && ch <= '9');
    }

    private static String zeroPad(String digits, int length) {
        return "0".repeat(length - digits.length()) + digits;
    }
}
