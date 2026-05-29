package com.shivam.aicodereviewer.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PRNotFoundException.class)
    public ResponseEntity<Map<String, String>>
        handlePRNotFound(PRNotFoundException ex) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "PR Not Found",
                "message", ex.getMessage()
            ));
    }

    @ExceptionHandler(PRNotOpenException.class)
    public ResponseEntity<Map<String, String>>
        handlePRNotOpen(PRNotOpenException ex) {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "PR Not Open",
                "message", ex.getMessage()
            ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>>
        handleUserNotFound(UserNotFoundException ex) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "User Not Found",
                "message", ex.getMessage()
            ));
    }

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<Map<String, String>>
        handleRepoNotFound(RepositoryNotFoundException ex) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "Repository Not Found",
                "message", ex.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>>
        handleGeneral(Exception ex) {

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage()
            ));
    }
}
