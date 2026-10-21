package org.prestiq.recommendation.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request parameters failed validation criteria."
        );
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("https://api.prestiq.org/errors/validation-failed"));
        problem.setProperty("timestamp", Instant.now());

        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        problem.setProperty("invalidParams", invalidParams);
        return problem;
    }

    private Map<String, String> formatFieldError(FieldError error) {
        Map<String, String> map = new HashMap<>();
        map.put("field", error.getField());
        map.put("message", error.getDefaultMessage());
        return map;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://api.prestiq.org/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StudentProfileNotFoundException.class)
    public ProblemDetail handleStudentProfileNotFound(StudentProfileNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Student Profile Not Found");
        problem.setType(URI.create("https://api.prestiq.org/errors/student-profile-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ProblemDetail handleRecommendationNotFound(RecommendationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recommendation Not Found");
        problem.setType(URI.create("https://api.prestiq.org/errors/recommendation-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ProblemDetail handleDownstreamService(DownstreamServiceException ex) {
        log.error("Downstream service communication failure with {}: {}", ex.getServiceName(), ex.getMessage());
        HttpStatus status = ex.getStatusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Downstream Service Error");
        problem.setType(URI.create("https://api.prestiq.org/errors/downstream-service-error"));
        problem.setProperty("downstreamService", ex.getServiceName());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Database constraint violation in recommendation-service: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A database constraint was violated."
        );
        problem.setTitle("Data Conflict");
        problem.setType(URI.create("https://api.prestiq.org/errors/data-conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Access denied: insufficient permissions to perform this operation."
        );
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://api.prestiq.org/errors/forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://api.prestiq.org/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled internal server error in recommendation-service: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please try again later."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.prestiq.org/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
