package com.example.test_framework_api.controller;

import com.example.test_framework_api.dto.AuthRequest;
import com.example.test_framework_api.dto.AuthResponse;
import com.example.test_framework_api.dto.RegisterRequest;
import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already exists"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        user.setRoles(roles);

        userRepository.save(user);
        log.info("New user registered: {}", request.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "username", user.getUsername()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(userDetails);
            
            User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

            log.info("User logged in: {}", request.getUsername());

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRoles()
            ));
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles()
        ));
    }
}