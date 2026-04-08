package com.sociolab.surehealth.exception;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

        log.warn("action=exception_handle status=FAILED errorType={} message={} path={}",
                ex.getErrorType(),
                ex.getMessage(),
                request.getRequestURI());

        return problemDetailFactory.fromAppException(ex, request.getRequestURI());
    }

    // ===== Validation error =====
    @ExceptionHandler({MethodArgumentNotValidException.class})
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

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                      HttpServletRequest request) {
        String message = ex.getAllErrors()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining("; "));

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
                message,
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
        log.warn("action=exception_handle status=FAILED errorType=ACCESS_DENIED message={} path={}",
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest request) {
        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = "Invalid value for parameter '" + paramName + "'. Expected type: " + expectedType;

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                  HttpServletRequest request) {
        String message = "Method " + ex.getMethod() + " not supported for this endpoint";

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {

        String message;
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife) {
            String fieldPath = ife.getPath() == null || ife.getPath().isEmpty()
                    ? "unknown"
                    : ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining("."));

            String value = ife.getValue() != null ? String.valueOf(ife.getValue()) : "null";
            message = "Invalid value '" + value + "' for field '" + fieldPath + "'";
        } else if (cause instanceof MismatchedInputException) {
            message = "Malformed JSON request";
        } else if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            message = "Request body is required";
        } else {
            message = "Invalid request payload";
        }

        log.warn("action=exception_handle status=FAILED errorType=VALIDATION_ERROR message={} path={}",
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

    // ===== Fallback / Internal Server Error =====
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {

        log.error("action=exception_handle status=FAILED errorType=INTERNAL_SERVER_ERROR message={} path={}",
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
