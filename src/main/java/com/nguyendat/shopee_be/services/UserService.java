package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.UserDto;
import com.nguyendat.shopee_be.entities.Role;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private AuthorityService authorityService;

    private UserDto toDto(User u) {
        if (u == null)
            return null;
    return UserDto.builder()
        .id(u.getId())
        .firstName(u.getFirstName())
        .lastName(u.getLastName())
        .email(u.getEmail())
        .avatar(u.getAvatar())
        .phoneNumber(u.getPhoneNumber())
        .enabled(u.isEnabled())
        .role(u.getPrimaryRole())
        .build();
    }

    public List<UserDto> findAll() {
        return userDetailRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDto findById(UUID id) {
        Optional<User> user = userDetailRepository.findById(id);
        return user.map(this::toDto).orElseThrow(() -> new ResourceNotFoundEx("User not found"));
    }

    public UserDto create(UserDto dto) {
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        user.setAuthorities(new ArrayList<>(List.of(authorityService.getRole(roleOf(dto)))));
        User saved = userDetailRepository.save(user);
        return toDto(saved);
    }

    public UserDto update(UUID id, UserDto dto) {

        logger.info("Updating user: {}", dto);
        User user = userDetailRepository.findById(id).orElseThrow(() -> new ResourceNotFoundEx("User not found"));
        if (dto.getFirstName() != null)
            user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)
            user.setLastName(dto.getLastName());
        if (dto.getEmail() != null)
            user.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null)
            user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
            logger.info("Updated avatar: {}", dto.getAvatar());
        }
        if (dto.getEnabled() != null)
            user.setEnabled(dto.getEnabled());
        if (dto.getRole() != null)
            user.setAuthorities(new ArrayList<>(List.of(authorityService.getRole(roleOf(dto)))));
        User saved = userDetailRepository.save(user);
        return toDto(saved);
    }

    // Chỉ chấp nhận USER / ADMIN, mặc định USER
    private static String roleOf(UserDto dto) {
        String role = Role.normalize(dto.getRole());
        if (role == null || role.isEmpty())
            return Role.USER;
        if (!Role.USER.equals(role) && !Role.ADMIN.equals(role))
            throw new IllegalArgumentException("Invalid role: " + dto.getRole());
        return role;
    }

    public void delete(UUID id) {
        User user = userDetailRepository.findById(id).orElseThrow(() -> new ResourceNotFoundEx("User not found"));
        // soft-delete: disable the account instead of removing the record
        user.setEnabled(false);
        userDetailRepository.save(user);
    }
}
