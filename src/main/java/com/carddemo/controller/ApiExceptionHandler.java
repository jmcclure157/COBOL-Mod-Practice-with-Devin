package com.carddemo.controller;

import com.carddemo.service.AccountNotFoundException;
import com.carddemo.service.InvalidAccountIdException;
import com.carddemo.service.CardOrAccountNotFoundException;
import com.carddemo.service.DuplicateTransactionIdException;
import com.carddemo.service.InvalidNewTransactionException;
import com.carddemo.service.InvalidTransactionIdException;
import com.carddemo.service.TransactionIdsExhaustedException;
import com.carddemo.service.InvalidTransactionListRequestException;
import com.carddemo.service.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns the messages COBOL showed in the screen's ERRMSG line into HTTP error responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidAccountIdException.class)
    public ProblemDetail invalidAccountId(InvalidAccountIdException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidTransactionListRequestException.class)
    public ProblemDetail invalidTransactionListRequest(InvalidTransactionListRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidTransactionIdException.class)
    public ProblemDetail invalidTransactionId(InvalidTransactionIdException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail accountNotFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail transactionNotFound(TransactionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidNewTransactionException.class)
    public ProblemDetail invalidNewTransaction(InvalidNewTransactionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CardOrAccountNotFoundException.class)
    public ProblemDetail cardOrAccountNotFound(CardOrAccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TransactionIdsExhaustedException.class)
    public ProblemDetail transactionIdsExhausted(TransactionIdsExhaustedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DuplicateTransactionIdException.class)
    public ProblemDetail duplicateTransactionId(DuplicateTransactionIdException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
