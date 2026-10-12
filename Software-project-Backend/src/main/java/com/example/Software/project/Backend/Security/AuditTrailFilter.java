package com.example.Software.project.Backend.Security;

import com.example.Software.project.Backend.Service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Records HTTP outcomes after controllers/services return; never reads bodies, uploads or tokens. */
public class AuditTrailFilter extends OncePerRequestFilter {
    private final AuditLogService audit;
    public AuditTrailFilter(AuditLogService audit) { this.audit = audit; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString(); // Server-generated, not a trusted incoming header.
        request.setAttribute(AuditLogService.REQUEST_ID, requestId);
        response.setHeader("X-Request-ID", requestId);
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException error) {
            failed = true;
            throw error;
        } finally {
            Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            String operation = handler instanceof HandlerMethod method
                    ? method.getBeanType().getSimpleName() + "." + method.getMethod().getName() : "UNMATCHED";
            int status = failed ? 500 : response.getStatus();
            String lower = operation.toLowerCase(Locale.ROOT);
            boolean sensitiveRead = lower.contains("export") || lower.contains("download") || lower.contains(".pdf")
                    || lower.contains("reportcontroller") || lower.equals("progresscontroller.snapshot");
            if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) || sensitiveRead || status >= 400) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                String actor = authentication != null && authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken) ? authentication.getName() : "ANONYMOUS";
                String outcome = status >= 200 && status < 300 ? "SUCCESS" : status == 401 || status == 403 ? "DENIED" : "FAILURE";
                Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                StringBuilder resource = new StringBuilder(pattern instanceof String route ? route : "UNMATCHED");
                Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (variables instanceof Map<?, ?> ids) ids.forEach((key, value) -> {
                    if (!key.toString().toLowerCase(Locale.ROOT).matches(".*(token|password|secret).*"))
                        resource.append("; ").append(key).append("=").append(AuditLogService.clean(String.valueOf(value)));
                });
                audit.record(actor, "HTTP " + operation, resource.toString(), outcome,
                        "method=" + request.getMethod() + "; status=" + status, requestId);
            }
        }
    }
}
