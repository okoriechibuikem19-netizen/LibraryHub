package com.libraryhub.api.exception;

import com.libraryhub.api.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex,HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND,ex.getMessage(),request,Map.of());
    }
    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ApiError> conflict(DuplicateResourceException ex,HttpServletRequest request) {
        return error(HttpStatus.CONFLICT,ex.getMessage(),request,Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,HttpServletRequest request) {
        Map<String,String> fields=new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST,"Validation failed",request,fields);
    }
    @ExceptionHandler(org.springframework.mail.MailException.class)
    ResponseEntity<ApiError> mailUnavailable(HttpServletRequest request){
        return error(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery failed. Please try again shortly or contact the library administrator.",request,Map.of());
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> configuration(IllegalStateException ex,HttpServletRequest request){
        return error(HttpStatus.SERVICE_UNAVAILABLE,ex.getMessage(),request,Map.of());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex,HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,ex.getMessage(),request,Map.of());
    }
    private ResponseEntity<ApiError> error(HttpStatus status,String message,HttpServletRequest request,Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(),status.value(),status.getReasonPhrase(),message,request.getRequestURI(),fields));
    }
}
