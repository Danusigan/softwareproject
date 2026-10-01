package com.example.Software.project.Backend.Config;

import com.example.Software.project.Backend.Model.UserAccess;
import com.example.Software.project.Backend.Repository.UserAccessRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class AccessTrackingInterceptor implements HandlerInterceptor {

    private final UserAccessRepository repo;

    public AccessTrackingInterceptor(UserAccessRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getName())) {
                String username = a.getName();
                LocalDateTime now = LocalDateTime.now();
                UserAccess ua = repo.findById(username).orElse(null);
                if (ua == null) {
                    ua = new UserAccess();
                    ua.setUsername(username);
                    ua.setFirstAccess(now);
                    ua.setLastAccess(now);
                    repo.save(ua);
                } else if (ua.getLastAccess() == null
                        || Duration.between(ua.getLastAccess(), now).getSeconds() > 60) {
                    ua.setLastAccess(now);
                    repo.save(ua);
                }
            }
        } catch (Exception ignored) {
            // tracking must never break a request
        }
        return true;
    }
}