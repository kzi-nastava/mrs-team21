package com.ftn.drumigo.config;

import jakarta.servlet.Servlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<?> h2ConsoleServletRegistration() {
        try {
            Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
            ServletRegistrationBean<?> registration = new ServletRegistrationBean<>(servlet);
            registration.addUrlMappings("/h2-console/*");
            return registration;
        } catch (Exception e) {
            throw new IllegalStateException(
                "H2 console servlet not available. Ensure H2 2.x is on the classpath.",
                e
            );
        }
    }
}
