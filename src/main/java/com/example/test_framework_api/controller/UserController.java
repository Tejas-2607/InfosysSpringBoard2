// package com.example.test_framework_api.controller;

// import com.example.test_framework_api.model.User;
// import com.example.test_framework_api.repository.UserRepository;
// import com.example.test_framework_api.repository.TestResultRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.stream.Collectors;

// @RestController
// @RequestMapping("/api/users")
// @RequiredArgsConstructor
// @Slf4j
// public class UserController {

//     private final UserRepository userRepository;
//     private final TestResultRepository testResultRepository;

//     /**
//      * Admin only: Get all users
//      */
//     @GetMapping
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> getAllUsers() {
//         List<User> users = userRepository.findAll();
        
//         var userList = users.stream().map(user -> Map.of(
//                 "id", user.getId(),
//                 "username", user.getUsername(),
//                 "email", user.getEmail(),
//                 "roles", user.getRoles(),
//                 "enabled", user.isEnabled(),
//                 "createdAt", user.getCreatedAt()
//         )).collect(Collectors.toList());

//         return ResponseEntity.ok(userList);
//     }

//     /**
//      * Admin only: Get user by ID with test execution history
//      */
//     @GetMapping("/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> getUserById(@PathVariable Long id) {
//         User user = userRepository.findById(id).orElse(null);
//         if (user == null) {
//             return ResponseEntity.notFound().build();
//         }

//         // Get user's test execution history
//         long totalTests = testResultRepository.findAll().stream()
//                 .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(id))
//                 .count();

//         return ResponseEntity.ok(Map.of(
//                 "id", user.getId(),
//                 "username", user.getUsername(),
//                 "email", user.getEmail(),
//                 "roles", user.getRoles(),
//                 "enabled", user.isEnabled(),
//                 "createdAt", user.getCreatedAt(),
//                 "testExecutions", totalTests
//         ));
//     }

//     /**
//      * Admin only: Promote user to admin
//      */
//     @PutMapping("/{id}/promote")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> promoteToAdmin(@PathVariable Long id) {
//         User user = userRepository.findById(id).orElse(null);
//         if (user == null) {
//             return ResponseEntity.notFound().build();
//         }

//         Set<String> roles = user.getRoles();
//         if (roles == null) {
//             roles = new HashSet<>();
//         }
//         roles.add("ROLE_ADMIN");
//         user.setRoles(roles);
//         userRepository.save(user);

//         log.info("User {} promoted to ADMIN", user.getUsername());

//         return ResponseEntity.ok(Map.of(
//                 "message", "User promoted to admin",
//                 "username", user.getUsername(),
//                 "roles", user.getRoles()
//         ));
//     }

//     /**
//      * Admin only: Demote admin to user
//      */
//     @PutMapping("/{id}/demote")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> demoteToUser(@PathVariable Long id) {
//         User user = userRepository.findById(id).orElse(null);
//         if (user == null) {
//             return ResponseEntity.notFound().build();
//         }

//         Set<String> roles = new HashSet<>();
//         roles.add("ROLE_USER");
//         user.setRoles(roles);
//         userRepository.save(user);

//         log.info("User {} demoted to USER", user.getUsername());

//         return ResponseEntity.ok(Map.of(
//                 "message", "Admin demoted to user",
//                 "username", user.getUsername(),
//                 "roles", user.getRoles()
//         ));
//     }

//     /**
//      * Get current user's test execution history
//      */
//     @GetMapping("/me/tests")
//     public ResponseEntity<?> getMyTests(org.springframework.security.core.Authentication authentication) {
//         String username = authentication.getName();
//         User user = userRepository.findByUsername(username).orElseThrow();

//         var myTests = testResultRepository.findAll().stream()
//                 .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(user.getId()))
//                 .map(result -> Map.of(
//                         "testName", result.getTestName(),
//                         "status", result.getStatus().toString(),
//                         "duration", result.getDuration() != null ? result.getDuration() : 0L,
//                         "createdAt", result.getCreatedAt()
//                 ))
//                 .collect(Collectors.toList());

//         return ResponseEntity.ok(Map.of(
//                 "username", username,
//                 "totalTests", myTests.size(),
//                 "tests", myTests
//         ));
//     }
// }

package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import com.example.test_framework_api.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final TestRunRepository testRunRepository;

    /**
     * Admin only: Get all users with test execution statistics
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        var userList = users.stream().map(user -> {
            // FIXED: Calculate test execution count properly
            long testExecutions = testResultRepository.findAll().stream()
                    .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(user.getId()))
                    .count();
            
            long testRunsCreated = testRunRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(user.getId()))
                    .count();

            return Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", user.getRoles(),
                    "enabled", user.isEnabled(),
                    "createdAt", user.getCreatedAt(),
                    "testExecutions", testExecutions,
                    "testRunsCreated", testRunsCreated
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    /**
     * Admin only: Get user by ID with detailed test execution history
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // FIXED: Properly count user's test executions
        long totalTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(id))
                .count();

        long passedTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(id))
                .filter(r -> r.getStatus().name().equals("PASSED"))
                .count();

        long failedTests = totalTests - passedTests;

        long testRunsCreated = testRunRepository.findAll().stream()
                .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(id))
                .count();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "enabled", user.isEnabled(),
                "createdAt", user.getCreatedAt(),
                "statistics", Map.of(
                        "totalTests", totalTests,
                        "passedTests", passedTests,
                        "failedTests", failedTests,
                        "testRunsCreated", testRunsCreated,
                        "passRate", totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0.0
                )
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
    public ResponseEntity<?> getMyTests(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // FIXED: Properly retrieve user's test results
        var myTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(user.getId()))
                .map(result -> Map.of(
                        "testName", result.getTestName(),
                        "status", result.getStatus().toString(),
                        "duration", result.getDuration() != null ? result.getDuration() : 0L,
                        "createdAt", result.getCreatedAt(),
                        "retryCount", result.getRetryCount() != null ? result.getRetryCount() : 0
                ))
                .collect(Collectors.toList());

        long passedCount = myTests.stream()
                .filter(t -> "PASSED".equals(t.get("status")))
                .count();

        return ResponseEntity.ok(Map.of(
                "username", username,
                "totalTests", myTests.size(),
                "passedTests", passedCount,
                "failedTests", myTests.size() - passedCount,
                "passRate", myTests.size() > 0 ? (passedCount * 100.0 / myTests.size()) : 0.0,
                "tests", myTests
        ));
    }

    /**
     * Get current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        long totalTests = testResultRepository.findAll().stream()
                .filter(r -> r.getExecutedBy() != null && r.getExecutedBy().getId().equals(user.getId()))
                .count();

        long testRunsCreated = testRunRepository.findAll().stream()
                .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(user.getId()))
                .count();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "statistics", Map.of(
                        "testExecutions", totalTests,
                        "testRunsCreated", testRunsCreated
                )
        ));
    }
}