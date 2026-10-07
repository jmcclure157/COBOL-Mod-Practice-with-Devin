package com.carddemo.service;

import java.util.List;

/** The COTRN00 screen: page number (PAGENUM), up to 10 rows, and whether PF8 (next page) has more. */
public record TransactionPage(int page, boolean hasNextPage, List<TransactionListItem> transactions) {
}
