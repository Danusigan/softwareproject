package com.example.Software.project.Backend.Security;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Validate configuration before any datasource connections are created. */
@Configuration(proxyBeanMethods = false)
public class SecretConfiguration {
    @Bean
    static BeanFactoryPostProcessor databaseSecretValidation(Environment environment) {
        return beanFactory -> {
            String url = required(environment, "spring.datasource.url");
            // Isolated H2 tests deliberately use a blank password.
            if (url.startsWith("jdbc:mysql:")) {
                required(environment, "spring.datasource.username");
                required(environment, "spring.datasource.password");
            }
        };
    }

    private static String required(Environment environment, String property) {
        String value;
        try {
            value = environment.getProperty(property);
        } catch (IllegalArgumentException unresolved) {
            // Never include property contents or the underlying exception in startup output.
            throw new IllegalStateException("Set " + property + " through the environment.");
        }
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException("Set " + property + " through the environment.");
        }
        return value;
    }
}
