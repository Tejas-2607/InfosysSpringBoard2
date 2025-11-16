// package com.example.test_framework_api.config;

// import com.example.test_framework_api.security.JwtAuthenticationFilter;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.AuthenticationProvider;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration
// @EnableWebSecurity
// @EnableMethodSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {

//   private final JwtAuthenticationFilter jwtAuthFilter;
//   private final UserDetailsService userDetailsService;

//   @Bean
//   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//     http
//         .csrf(AbstractHttpConfigurer::disable)
//         .cors(AbstractHttpConfigurer::disable)
//         // .csrf(csrf -> csrf.disable())
//         // .authorizeHttpRequests(auth -> auth
//         // .anyRequest().authenticated()
//         // )
//         // .formLogin(login -> login.permitAll())
//         // .userDetailsService(userDetailsService);
//         .authorizeHttpRequests(auth -> auth
//             // Public endpoints
//             .requestMatchers("/api/auth/**").permitAll()

//             // Admin-only endpoints
//             .requestMatchers("/api/runs").hasRole("ADMIN")
//             .requestMatchers("/api/runs/reports").hasRole("ADMIN")
//             .requestMatchers("/api/status").hasRole("ADMIN")
//             .requestMatchers("/api/runs/metrics").hasRole("ADMIN")

//             // User and Admin accessible endpoints
//             .requestMatchers("/api/suites/**").authenticated()
//             .requestMatchers("/test-element/**").authenticated()
//             .requestMatchers("/api/runs/**").authenticated()

//             // Static resources
//             .requestMatchers("/reports/**").permitAll()

//             .anyRequest().authenticated())
//         .sessionManagement(session -> session
//             .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//         .authenticationProvider(authenticationProvider())
//         .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

//     return http.build();
//   }

//   @Bean
//   public AuthenticationProvider authenticationProvider() {
//     DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//     authProvider.setUserDetailsService(userDetailsService);
//     authProvider.setPasswordEncoder(passwordEncoder());
//     return authProvider;
//   }

//   @Bean
//   public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//     return config.getAuthenticationManager();
//   }

//   @Bean
//   public PasswordEncoder passwordEncoder() {
//     return new BCryptPasswordEncoder();
//   }
// }

package com.example.test_framework_api.config;

import com.example.test_framework_api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
// import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // FIXED: Proper CORS config
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/api/auth/register").permitAll()
            .requestMatchers("/api/auth/me").authenticated()

            // Admin-only endpoints
            .requestMatchers("/api/runs").hasRole("ADMIN")
            .requestMatchers("/api/runs/reports").hasRole("ADMIN")
            .requestMatchers("/api/status").hasRole("ADMIN")
            .requestMatchers("/api/runs/metrics").hasRole("ADMIN")
            .requestMatchers("/api/users/**").hasRole("ADMIN") // FIXED: Admin user management

            // User and Admin accessible endpoints
            .requestMatchers("/api/suites/**").authenticated()
            .requestMatchers("/test-element/**").authenticated()
            .requestMatchers("/api/runs/**").authenticated()

            // Static resources
            .requestMatchers("/reports/**").permitAll()
            .requestMatchers("/static/**").permitAll()
            .requestMatchers("/favicon.ico").permitAll()

            .anyRequest().authenticated())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // FIXED: Frontend CORS configuration
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",      // Vite default dev server
        "http://127.0.0.1:5173"      // Alternative React port
    ));
    
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "Accept",
        "X-Requested-With",
        "Origin"
    ));
    
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type"
    ));
    
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}