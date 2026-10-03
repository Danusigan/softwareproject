package com.example.Software.project.Backend.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessTrackingInterceptor tracker;

    public WebConfig(AccessTrackingInterceptor tracker) {
        this.tracker = tracker;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tracker).addPathPatterns("/api/**");
    }
}