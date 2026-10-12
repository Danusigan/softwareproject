package com.example.Software.project.Backend.Security;

import com.example.Software.project.Backend.Service.AuditLogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.LoggerFactory;

/** Never treats an arbitrary library/database exception message as public text. */
public final class SafeErrors {
    private SafeErrors() {}

    public static HttpStatus status(Throwable error) {
        if (error instanceof MaxUploadSizeExceededException) return HttpStatus.PAYLOAD_TOO_LARGE;
        if (error instanceof AccessDeniedException) return HttpStatus.FORBIDDEN;
        if (error instanceof InternalAuthenticationServiceException) return HttpStatus.INTERNAL_SERVER_ERROR;
        if (error instanceof AuthenticationException || error instanceof io.jsonwebtoken.JwtException) return HttpStatus.UNAUTHORIZED;
        if (error instanceof DataIntegrityViolationException) return HttpStatus.CONFLICT;
        if (error instanceof ResourceNotFoundException) return HttpStatus.NOT_FOUND;
        if (error instanceof ResponseStatusException response) {
            HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
            return status != null && status.isError() ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (error instanceof IllegalArgumentException) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public static String message(Throwable error) {
        if (error instanceof PublicValidationException && error.getMessage() != null) {
            String message = error.getMessage().replaceAll("[\\p{Cntrl}\\p{Cf}]", " ");
            return message.substring(0, Math.min(500, message.length()));
        }
        return message(status(error));
    }

    public static String message(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request. Check the supplied values.";
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            case NOT_FOUND -> "The requested resource was not found.";
            case CONFLICT -> "The request conflicts with an existing record.";
            case METHOD_NOT_ALLOWED -> "HTTP method not supported for this endpoint";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported request content type.";
            case NOT_ACCEPTABLE -> "The requested response format is not supported.";
            case PAYLOAD_TOO_LARGE -> "Uploaded file exceeds the maximum allowed size";
            case LOCKED -> "Account temporarily locked. Please try again later.";
            default -> status.is5xxServerError() ? "An unexpected error occurred. Please try again later." : "The request could not be completed.";
        };
    }

    public static String requestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object value = attributes.getRequest().getAttribute(AuditLogService.REQUEST_ID);
            if (value instanceof String id && id.matches("[0-9a-fA-F-]{36}")) return id;
        }
        return "unavailable";
    }

    public static void log(Throwable error) {
        // Do not log exception text, SQL, request values, tokens or a cause/stack trace.
        LoggerFactory.getLogger(SafeErrors.class).warn("Request failed: requestId={}, type={}",
                requestId(), error.getClass().getSimpleName());
    }

    public static void log(Throwable error, jakarta.servlet.http.HttpServletRequest request) {
        Object value = request.getAttribute(AuditLogService.REQUEST_ID);
        String id = value instanceof String text && text.matches("[0-9a-fA-F-]{36}") ? text : "unavailable";
        LoggerFactory.getLogger(SafeErrors.class).warn("Request failed: requestId={}, type={}", id, error.getClass().getSimpleName());
    }

    public static void write(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response, HttpStatus status) throws java.io.IOException {
        Object attribute = request.getAttribute(AuditLogService.REQUEST_ID);
        String id = attribute instanceof String value && value.matches("[0-9a-fA-F-]{36}") ? value : "unavailable";
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getWriter(),
                java.util.Map.of("status", "ERROR", "message", message(status), "requestId", id));
    }
}
