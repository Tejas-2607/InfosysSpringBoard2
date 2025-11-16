package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;

    /**
     * Admin only: Get all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        var userList = users.stream().map(user -> Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "enabled", user.isEnabled(),
                "createdAt", user.getCreatedAt()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    /**
     * Admin only: Get user by ID with test execution history
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Get user's test execution history
        long totalTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(id))
                .count();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "enabled", user.isEnabled(),
                "createdAt", user.getCreatedAt(),
                "testExecutions", totalTests
        ));
    }

    /**
     * Admin only: Promote user to admin
     */
    @PutMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Set<String> roles = user.getRoles();
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add("ROLE_ADMIN");
        user.setRoles(roles);
        userRepository.save(user);

        log.info("User {} promoted to ADMIN", user.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User promoted to admin",
                "username", user.getUsername(),
                "roles", user.getRoles()
        ));
    }

    /**
     * Admin only: Demote admin to user
     */
    @PutMapping("/{id}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> demoteToUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        user.setRoles(roles);
        userRepository.save(user);

        log.info("User {} demoted to USER", user.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "Admin demoted to user",
                "username", user.getUsername(),
                "roles", user.getRoles()
        ));
    }

    /**
     * Get current user's test execution history
     */
    @GetMapping("/me/tests")
    public ResponseEntity<?> getMyTests(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        var myTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(user.getId()))
                .map(result -> Map.of(
                        "testName", result.getTestName(),
                        "status", result.getStatus().toString(),
                        "duration", result.getDuration() != null ? result.getDuration() : 0L,
                        "createdAt", result.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "username", username,
                "totalTests", myTests.size(),
                "tests", myTests
        ));
    }
}