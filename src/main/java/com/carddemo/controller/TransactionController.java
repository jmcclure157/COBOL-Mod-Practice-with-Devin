package com.carddemo.controller;

import com.carddemo.service.TransactionListService;
import com.carddemo.service.TransactionPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST replacement for the COTRN00 transaction list screen (CICS transaction CT00). */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionListService transactionListService;

    public TransactionController(TransactionListService transactionListService) {
        this.transactionListService = transactionListService;
    }

    @GetMapping
    public TransactionPage listTransactions(@RequestParam(required = false) String startId,
                                            @RequestParam(required = false) String page) {
        return transactionListService.listTransactions(startId, page);
    }
}
