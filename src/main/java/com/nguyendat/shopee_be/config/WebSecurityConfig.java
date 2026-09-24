package com.nguyendat.shopee_be.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nguyendat.shopee_be.entities.Role;
import com.nguyendat.shopee_be.exceptions.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    // Không cần đăng nhập
    private static final String[] PUBLIC_APIS = {
            "/api/auth/**",
            "/api/health",
            "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/api/files/**", "/files/**",
            "/api/upload/**", "/uploads/**",
            "/api/vnpay/return",
            "/ws/**"
    };

    // Chỉ cho phép xem (GET) khi chưa đăng nhập
    private static final String[] PUBLIC_READ_APIS = {
            "/api/products/**",
            "/api/category/**",
            "/api/category-types/**",
            "/api/reviews/**"
    };

    // Toàn bộ endpoint chỉ ADMIN được gọi
    private static final String[] ADMIN_APIS = {
            "/api/admin/**",
            "/api/dashboard/**",
            "/api/inventory-receipts/**",
            "/api/inventory-receipt-items/**",
            "/api/stock-movements/**"
    };

    // Endpoint catalog: ai cũng xem được, chỉ ADMIN được thêm/sửa/xoá
    private static final String[] ADMIN_WRITE_APIS = {
            "/api/products/**",
            "/api/category/**",
            "/api/category-types/**",
            "/api/product-variants/**",
            "/api/product-statuses/**"
    };

    private final UserDetailsService userDetailsService;
    private final JWTTokenHelper jwtTokenHelper;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://ecommerce-fe-pink-one.vercel.app}")
    private String corsAllowedOrigins; // comma separated

    public WebSecurityConfig(UserDetailsService userDetailsService, JWTTokenHelper jwtTokenHelper) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenHelper = jwtTokenHelper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_APIS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_APIS).permitAll()
                        .requestMatchers(ADMIN_APIS).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.POST, ADMIN_WRITE_APIS).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PUT, ADMIN_WRITE_APIS).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, ADMIN_WRITE_APIS).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, ADMIN_WRITE_APIS).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole(Role.ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> writeError(request, response,
                                HttpStatus.UNAUTHORIZED, "You are not authorized to access this resource"))
                        .accessDeniedHandler((request, response, ex) -> writeError(request, response,
                                HttpStatus.FORBIDDEN, "You do not have permission to access this resource")))
                .addFilterBefore(new JWTAuthenticationFilter(jwtTokenHelper, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    // Mặc định BCrypt, vẫn đọc được mật khẩu cũ có tiền tố {bcrypt}
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        for (String origin : corsAllowedOrigins.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                cors.addAllowedOrigin(trimmed);
            }
        }
        cors.addAllowedHeader("*");
        cors.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
