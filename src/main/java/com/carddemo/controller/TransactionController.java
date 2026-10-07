package com.carddemo.controller;

import com.carddemo.service.TransactionListService;
import com.carddemo.service.TransactionPage;
import com.carddemo.service.TransactionView;
import com.carddemo.service.TransactionViewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST replacement for the COTRN00 transaction list (CT00) and COTRN01 view transaction (CT01) screens. */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionListService transactionListService;
    private final TransactionViewService transactionViewService;

    public TransactionController(TransactionListService transactionListService,
                                 TransactionViewService transactionViewService) {
        this.transactionListService = transactionListService;
        this.transactionViewService = transactionViewService;
    }

    @GetMapping
    public TransactionPage listTransactions(@RequestParam(required = false) String startId,
                                            @RequestParam(required = false) String page) {
        return transactionListService.listTransactions(startId, page);
    }

    @GetMapping("/{id}")
    public TransactionView viewTransaction(@PathVariable String id) {
        return transactionViewService.viewTransaction(id);
    }
}
