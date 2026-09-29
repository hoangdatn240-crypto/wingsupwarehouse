package com.wingsup.warehouse.security;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException e
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message",
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Dữ liệu không hợp lệ"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(
            MethodArgumentNotValidException e
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message",
                        "Dữ liệu không hợp lệ: thiếu trường bắt buộc"
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(
            DataIntegrityViolationException e
    ) {

        e.printStackTrace();

        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        message != null
                                ? message
                                : "Lỗi dữ liệu trong cơ sở dữ liệu"
                ));
    }
}