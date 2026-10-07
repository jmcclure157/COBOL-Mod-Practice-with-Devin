package com.carddemo.batch;

/** A CBTRN02C file error that ends the job (9999-ABEND-PROGRAM), as opposed to a rejected record. */
public class PostingAbendException extends RuntimeException {

    public PostingAbendException(String message, Throwable cause) {
        super(message, cause);
    }
}
