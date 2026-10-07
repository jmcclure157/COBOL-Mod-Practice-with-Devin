package com.carddemo.controller;

import com.carddemo.service.AccountView;
import com.carddemo.service.AccountViewService;
import com.carddemo.service.BillPaymentRequest;
import com.carddemo.service.BillPaymentResult;
import com.carddemo.service.BillPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** REST replacement for the COACTVW account view (CAVW) and COBIL00 bill payment (CB00) screens. */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountViewService accountViewService;
    private final BillPaymentService billPaymentService;

    public AccountController(AccountViewService accountViewService, BillPaymentService billPaymentService) {
        this.accountViewService = accountViewService;
        this.billPaymentService = billPaymentService;
    }

    @GetMapping("/{id}")
    public AccountView viewAccount(@PathVariable String id) {
        return accountViewService.viewAccount(id);
    }

    /** COBIL00C: pay the account's whole current balance. Requires {"confirm": "Y"}, like the screen did. */
    @PostMapping("/{id}/payments")
    public ResponseEntity<BillPaymentResult> payBill(@PathVariable String id,
                                                     @RequestBody(required = false) BillPaymentRequest request) {
        BillPaymentResult result = billPaymentService.payBill(id, request == null ? null : request.confirm());
        return ResponseEntity.created(URI.create("/transactions/" + result.transactionId())).body(result);
    }
}
