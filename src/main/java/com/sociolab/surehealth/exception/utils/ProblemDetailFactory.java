package com.sociolab.surehealth.exception.utils;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class ProblemDetailFactory {

    private final String baseUri;
    private final String apiVersion;
    private final Clock clock;

    // Constructor allows configuration for baseUri, version, and testable clock
    public ProblemDetailFactory(String baseUri, String apiVersion, Clock clock) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
        this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion must not be null");
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== FLUENT BUILDER ====================
    public static class Builder {
        private ErrorType errorType;
        private HttpStatus status;
        private String title;
        private String detail;
        private String path;
        private String typeSuffix;
        private Clock clock;
        private String baseUri;
        private String apiVersion;

        private Builder() {}

        public Builder errorType(ErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder status(HttpStatus status) {
            this.status = status;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder typeSuffix(String typeSuffix) {
            this.typeSuffix = typeSuffix;
            return this;
        }

        public Builder baseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public ProblemDetail build() {
            // Determine status
            HttpStatus httpStatus = status;
            if (httpStatus == null && errorType != null) {
                httpStatus = HttpStatus.valueOf(errorType.getHttpCode());
            }

            // Determine title / detail
            String finalTitle = title != null ? title : (errorType != null ? errorType.name() : "Error");
            String finalDetail = detail != null ? detail : (errorType != null ? errorType.getMessage() : "Unexpected error");

            // Determine type URI
            String finalType = typeSuffix != null ? typeSuffix : (errorType != null ? errorType.name() : "UNKNOWN");
            String finalBaseUri = baseUri != null ? baseUri : "https://surehealth/errors/";

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR, finalDetail);
            problem.setTitle(finalTitle);
            problem.setType(URI.create(finalBaseUri + finalType));

            // Properties
            String traceId = MDC.get("traceId");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }
            problem.setProperty("traceId", traceId);
            problem.setProperty("version", apiVersion != null ? apiVersion : "v1");
            problem.setProperty("timestamp", OffsetDateTime.now(clock));
            if (path != null) {
                problem.setProperty("path", path);
            }

            return problem;
        }
    }

    // ==================== FROM AppException ====================
    public ProblemDetail fromAppException(AppException ex, String path) {
        return ProblemDetailFactory.builder()
                .errorType(ex.getErrorType())
                .title(ex.getErrorType().name())
                .detail(ex.getDetailMessage() != null ? ex.getDetailMessage() : ex.getErrorType().getMessage())
                .path(path)
                .baseUri(baseUri)
                .apiVersion(apiVersion)
                .clock(clock)
                .build();
    }
}