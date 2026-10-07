package com.carddemo.controller;

import com.carddemo.service.AccountView;
import com.carddemo.service.AccountViewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST replacement for the COACTVW account view screen (CICS transaction CAVW). */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountViewService accountViewService;

    public AccountController(AccountViewService accountViewService) {
        this.accountViewService = accountViewService;
    }

    @GetMapping("/{id}")
    public AccountView viewAccount(@PathVariable String id) {
        return accountViewService.viewAccount(id);
    }
}
