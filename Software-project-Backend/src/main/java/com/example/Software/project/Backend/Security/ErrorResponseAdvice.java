package com.example.Software.project.Backend.Security;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keep existing response contracts; add correlation and prevent caching error responses. */
@RestControllerAdvice
public class ErrorResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter type, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servlet && servlet.getServletResponse().getStatus() >= 400) {
            response.getHeaders().setCacheControl("no-store");
            if (body instanceof Map<?, ?> original) {
                Map<String, Object> safe = new LinkedHashMap<>();
                original.forEach((key, value) -> safe.put(String.valueOf(key), value));
                safe.put("status", "ERROR");
                safe.put("requestId", SafeErrors.requestId());
                return safe;
            }
        }
        return body;
    }
}
