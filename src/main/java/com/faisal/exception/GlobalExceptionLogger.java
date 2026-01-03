package com.faisal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionLogger {

    @ExceptionHandler(Exception.class)
    public void logUnhandled(Exception ex) throws Exception {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        throw ex;
    }
}