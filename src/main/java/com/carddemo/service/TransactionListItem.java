package com.carddemo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the COTRN00 screen: TRNIDnn, TDATEnn, TDESCnn, TAMTnnn. */
public record TransactionListItem(String transactionId, LocalDate date, String description, BigDecimal amount) {
}
