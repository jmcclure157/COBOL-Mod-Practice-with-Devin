package com.carddemo.controller;

import com.carddemo.service.NewTransactionRequest;
import com.carddemo.service.NewTransactionResult;
import com.carddemo.service.TransactionAddService;
import com.carddemo.service.TransactionListService;
import com.carddemo.service.TransactionPage;
import com.carddemo.service.TransactionView;
import com.carddemo.service.TransactionViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** REST replacement for the COTRN00 list (CT00), COTRN01 view (CT01) and COTRN02 add (CT02) transaction screens. */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionListService transactionListService;
    private final TransactionViewService transactionViewService;
    private final TransactionAddService transactionAddService;

    public TransactionController(TransactionListService transactionListService,
                                 TransactionViewService transactionViewService,
                                 TransactionAddService transactionAddService) {
        this.transactionListService = transactionListService;
        this.transactionViewService = transactionViewService;
        this.transactionAddService = transactionAddService;
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

    @PostMapping
    public ResponseEntity<NewTransactionResult> addTransaction(@RequestBody NewTransactionRequest request) {
        NewTransactionResult result = transactionAddService.addTransaction(request);
        return ResponseEntity.created(URI.create("/transactions/" + result.transactionId())).body(result);
    }
}
