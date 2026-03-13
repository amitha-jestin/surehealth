package com.sociolab.surehealth.exception;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    // ===== Custom business exception =====
    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex, HttpServletRequest request) {

        log.warn("BUSINESS_EXCEPTION: type={} message={} path={}",
                ex.getErrorType(),
                ex.getMessage(),
                request.getRequestURI());

        return problemDetailFactory.fromAppException(ex, request.getRequestURI());
    }

    // ===== Validation error =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.toList());

        String message = errors.isEmpty()
                ? "Validation failed"
                : String.join("; ", errors);

        log.warn("VALIDATION_EXCEPTION: message={} path={}",
                message,
                request.getRequestURI());

        return ProblemDetailFactory.builder()
                .errorType(ErrorType.VALIDATION_ERROR)
                .title(ErrorType.VALIDATION_ERROR.name())
                .detail(message)
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();
    }

    // ===== Constraint violation =====
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex,
                                          HttpServletRequest request) {

        log.warn("CONSTRAINT_EXCEPTION: message={} path={}",
                ex.getMessage(),
                request.getRequestURI());

        return ProblemDetailFactory.builder()
                .errorType(ErrorType.VALIDATION_ERROR)
                .title(ErrorType.VALIDATION_ERROR.name())
                .detail(ex.getMessage())
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAuthorizationDenied(
            Exception ex,
            HttpServletRequest request) {
        log.warn("AUTHORIZATION_DENIED_EXCEPTION: message={} path={}",
                ex.getMessage(),
                request.getRequestURI());

        return ProblemDetailFactory.builder()
                .errorType(ErrorType.ACCESS_DENIED)
                .title(ErrorType.ACCESS_DENIED.name())
                .detail(ex.getMessage())
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();
    }

    // ===== Fallback / Internal Server Error =====
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {

        log.error("UNEXPECTED_EXCEPTION: message={} path={}",
                ex.getMessage(),
                request.getRequestURI(),
                ex);

        return ProblemDetailFactory.builder()
                .errorType(ErrorType.INTERNAL_SERVER_ERROR)
                .title(ErrorType.INTERNAL_SERVER_ERROR.name())
                .detail(ex.getMessage() != null
                        ? ex.getMessage()
                        : "Unexpected internal error")
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();
    }
}