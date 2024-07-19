package edu.stanford.slac.elog_plus.api.v1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class HandleValidationError {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> sb.append("[%s] %s: %s".formatted(error.getObjectName(),error.getField(), error.getDefaultMessage())));
        Map<String, String> errors = new HashMap<>();
        errors.put("errorCode", "-1");
        errors.put("errorMessage", sb.toString());
        errors.put("errorDomain", "binding errors");
        return ResponseEntity.badRequest().body(
                errors
        );
    }
}