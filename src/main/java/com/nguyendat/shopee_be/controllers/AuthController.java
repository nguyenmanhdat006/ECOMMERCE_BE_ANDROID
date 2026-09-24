package com.nguyendat.shopee_be.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nguyendat.shopee_be.dto.LoginRequest;
import com.nguyendat.shopee_be.dto.LoginResponse;
import com.nguyendat.shopee_be.dto.RegistrationRequest;
import com.nguyendat.shopee_be.dto.RegistrationResponse;
import com.nguyendat.shopee_be.services.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Sai email/mật khẩu hoặc tài khoản chưa kích hoạt -> 401 (GlobalExceptionHandler)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
        RegistrationResponse registrationResponse = authService.register(request);
        return new ResponseEntity<>(registrationResponse,
                registrationResponse.getCode() == 200 ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyCode(@RequestBody Map<String, String> map) {
        boolean verified = authService.verify(map.get("userName"), map.get("code"));
        return new ResponseEntity<>(verified ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
