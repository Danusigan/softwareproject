package com.example.Software.project.Backend.Security;

import org.springframework.http.*;
import org.springframework.web.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<?> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(Map.of("message", message, "status", "ERROR", "requestId", SafeErrors.requestId()));
    }

    @ExceptionHandler({PublicValidationException.class, IllegalArgumentException.class,
            AccessDeniedException.class, AuthenticationException.class,
            DataIntegrityViolationException.class, ResponseStatusException.class,
            MaxUploadSizeExceededException.class, ResourceNotFoundException.class})
    public ResponseEntity<?> handleKnown(Exception error) {
        SafeErrors.log(error);
        return response(SafeErrors.status(error), SafeErrors.message(error));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            MissingServletRequestPartException.class, MissingServletRequestParameterException.class,
            org.springframework.web.bind.ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MultipartException.class, jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<?> handleBadRequest(Exception error) {
        return response(HttpStatus.BAD_REQUEST, "Malformed or incomplete request. Check the supplied fields.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethod(HttpRequestMethodNotSupportedException error) {
        var result = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).cacheControl(CacheControl.noStore());
        if (error.getSupportedHttpMethods() != null) result.allow(error.getSupportedHttpMethods().toArray(HttpMethod[]::new));
        return result.body(Map.of("message", SafeErrors.message(HttpStatus.METHOD_NOT_ALLOWED), "status", "ERROR"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleMedia(HttpMediaTypeNotSupportedException error) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, SafeErrors.message(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<?> handleAccept(HttpMediaTypeNotAcceptableException error) {
        return response(HttpStatus.NOT_ACCEPTABLE, SafeErrors.message(HttpStatus.NOT_ACCEPTABLE));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<?> handleMissing(Exception error) {
        return response(HttpStatus.NOT_FOUND, SafeErrors.message(HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUncaught(Exception error) {
        SafeErrors.log(error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, SafeErrors.message(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
