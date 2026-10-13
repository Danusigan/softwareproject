package com.example.Software.project.Backend.Security;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;

/** Identity and authorities established by the filter from the current database account. */
public final class CurrentUser {
    private CurrentUser() {}
    public static String role() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return "";
        return auth.getAuthorities().stream().map(a -> a.getAuthority().trim().toLowerCase(java.util.Locale.ROOT))
                .filter(Set.of("superadmin", "admin", "lecture")::contains).findFirst().orElse("");
    }
    public static boolean admin() { return Set.of("admin", "superadmin").contains(role()); }
    public static String username() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
    }
}
