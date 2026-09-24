package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.Authority;
import com.nguyendat.shopee_be.entities.Role;
import com.nguyendat.shopee_be.repositories.AuthorityRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthorityService {

    private static final Map<String, String> DEFAULT_ROLES = Map.of(
            Role.USER, "Customer",
            Role.ADMIN, "Administrator");

    private final AuthorityRepository authorityRepository;

    public AuthorityService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    // Đảm bảo bảng AUTH_AUTHORITY luôn có đủ role mặc định
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultRoles() {
        DEFAULT_ROLES.forEach(this::getOrCreate);
    }

    // Tìm role theo mã (chấp nhận cả dữ liệu cũ dạng "ROLE_ADMIN"), chưa có thì tạo mới
    @Transactional
    public Authority getOrCreate(String role, String description) {
        String code = Role.normalize(role);
        return authorityRepository.findAll().stream()
                .filter(a -> code.equals(Role.normalize(a.getRoleCode())))
                .findFirst()
                .orElseGet(() -> authorityRepository.save(
                        Authority.builder().roleCode(code).roleDescription(description).build()));
    }

    public Authority getRole(String role) {
        String code = Role.normalize(role);
        return getOrCreate(code, DEFAULT_ROLES.getOrDefault(code, code));
    }
}
