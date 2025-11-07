package com.example.test_framework_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:reports/");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Apply to all endpoints
                .allowedOrigins("http://localhost:5173")  // Your Vite dev server origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Common HTTP methods
                .allowedHeaders("*")  // Allow all headers (e.g., Content-Type, Authorization)
                .allowCredentials(false);  // Set to true if you need cookies/auth
    }
}