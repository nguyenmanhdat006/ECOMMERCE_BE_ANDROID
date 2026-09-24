package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.config.JWTTokenHelper;
import com.nguyendat.shopee_be.dto.LoginRequest;
import com.nguyendat.shopee_be.dto.LoginResponse;
import com.nguyendat.shopee_be.dto.RegistrationRequest;
import com.nguyendat.shopee_be.dto.RegistrationResponse;
import com.nguyendat.shopee_be.dto.UserDto;
import com.nguyendat.shopee_be.entities.Role;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.ultil.VerificationCodeGenerator;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final AuthenticationManager authenticationManager;
    private final UserDetailRepository userDetailRepository;
    private final AuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JWTTokenHelper jwtTokenHelper;

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailRepository userDetailRepository,
                       AuthorityService authorityService,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       JWTTokenHelper jwtTokenHelper) {
        this.authenticationManager = authenticationManager;
        this.userDetailRepository = userDetailRepository;
        this.authorityService = authorityService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtTokenHelper = jwtTokenHelper;
    }

    // Đăng nhập bằng email + mật khẩu, trả về JWT
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getUserName());
        String password = request.getPassword() == null ? "" : request.getPassword().toString();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (DisabledException e) {
            throw new BadCredentialsException("Account is not activated");
        }

        User user = (User) authentication.getPrincipal();
        return LoginResponse.builder()
                .token(jwtTokenHelper.generateToken(user))
                .user(toDto(user))
                .build();
    }

    // Đăng ký tài khoản mới (role USER), gửi mã xác thực qua email
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword().toString();

        if (email.isEmpty() || !email.contains("@")) {
            return RegistrationResponse.builder().code(400).message("Email is invalid!").build();
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return RegistrationResponse.builder().code(400)
                    .message("Password must be at least " + MIN_PASSWORD_LENGTH + " characters!").build();
        }
        if (userDetailRepository.findByEmail(email) != null) {
            return RegistrationResponse.builder().code(400).message("Email already exist!").build();
        }

        Date now = new Date();
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(password))
                .provider("manual")
                .enabled(false)
                .verificationCode(VerificationCodeGenerator.generateCode())
                .authorities(new ArrayList<>(List.of(authorityService.getRole(Role.USER))))
                .createdOn(now)
                .updatedOn(now)
                .build();
        userDetailRepository.save(user);

        String mailResult = emailService.sendMail(user);
        logger.info("Registered user {} - {}", email, mailResult);

        return RegistrationResponse.builder().code(200).message("User created!").build();
    }

    // Kích hoạt tài khoản bằng mã xác thực đã gửi qua email
    @Transactional
    public boolean verify(String userName, String code) {
        User user = userDetailRepository.findByEmail(normalizeEmail(userName));
        if (user == null || code == null || user.getVerificationCode() == null
                || !user.getVerificationCode().equals(code.trim())) {
            return false;
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setUpdatedOn(new Date());
        userDetailRepository.save(user);
        return true;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .avatar(user.getAvatar())
                .role(user.getPrimaryRole())
                .build();
    }
}
