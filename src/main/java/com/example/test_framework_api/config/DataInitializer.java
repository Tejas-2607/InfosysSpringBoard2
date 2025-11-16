package com.example.test_framework_api.config;

import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // Create default admin user if not exists
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123")); // Change in production!
                admin.setEmail("admin@testframework.com");
                
                Set<String> roles = new HashSet<>();
                roles.add("ROLE_ADMIN");
                roles.add("ROLE_USER");
                admin.setRoles(roles);
                
                userRepository.save(admin);
                log.info("✓ Created default admin user: username=admin");
            }

            // Create default test user if not exists
            if (!userRepository.existsByUsername("testuser")) {
                User testUser = new User();
                testUser.setUsername("testuser");
                testUser.setPassword(passwordEncoder.encode("test123"));
                testUser.setEmail("testuser@testframework.com");
                
                Set<String> roles = new HashSet<>();
                roles.add("ROLE_USER");
                testUser.setRoles(roles);
                
                userRepository.save(testUser);
                log.info("✓ Created default test user: username=testuser");
            }
        };
    }
}