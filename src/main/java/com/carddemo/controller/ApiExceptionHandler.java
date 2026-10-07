package com.carddemo.controller;

import com.carddemo.service.AccountNotFoundException;
import com.carddemo.service.InvalidAccountIdException;
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

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail accountNotFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
