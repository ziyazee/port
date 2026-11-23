package com.porter.ksa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = new ArrayList<>();
        
        if ("dev".equals(activeProfile)) {
            allowedOrigins.add("http://localhost:*");
            allowedOrigins.add("http://127.0.0.1:*");
            allowedOrigins.add("http://192.168.*:*");
        }
        
        allowedOrigins.add("https://maps-500620.web.app");
        allowedOrigins.add("https://maps-500620.firebaseapp.com");
        
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}