package com.nguyendat.shopee_be.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Đọc JWT từ header, nếu hợp lệ thì gắn user vào SecurityContext.
// Token sai/hết hạn chỉ bỏ qua -> request được xử lý như chưa đăng nhập (401 nếu endpoint yêu cầu đăng nhập).
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTTokenHelper jwtTokenHelper;
    private final UserDetailsService userDetailsService;

    public JWTAuthenticationFilter(JWTTokenHelper jwtTokenHelper, UserDetailsService userDetailsService) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtTokenHelper.getToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String userName = jwtTokenHelper.getUserNameFromToken(token);
            if (userName != null) {
                authenticate(userName, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String userName, HttpServletRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            return;
        }

        // Tài khoản bị khoá/chưa kích hoạt thì token cũ không còn dùng được
        if (!userDetails.isEnabled()) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
