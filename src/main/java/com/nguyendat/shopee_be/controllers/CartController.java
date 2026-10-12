package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.dto.CartResponseDto;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.services.CartService;
import com.nguyendat.shopee_be.ultil.AccessUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin
@Tag(name = "Carts", description = "Manage shopping carts")
public class CartController {

    @Autowired
    private CartService cartService;

    // Chỉ ADMIN (cấu hình trong WebSecurityConfig)
    @GetMapping
    public ResponseEntity<List<CartResponseDto>> getAll() {
        return new ResponseEntity<>(cartService.findAll(), HttpStatus.OK);
    }

    // Chủ giỏ hoặc ADMIN
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartResponseDto>> getByUserId(@PathVariable UUID userId, @AuthenticationPrincipal User user) {
        AccessUtil.requireOwnerOrAdmin(user, userId);
        return new ResponseEntity<>(cartService.findByUserId(userId), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        CartDto dto = cartService.findById(id);
        AccessUtil.requireOwnerOrAdmin(user, dto.getUserId());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    // Thêm vào giỏ của người đang đăng nhập
    @PostMapping
    public ResponseEntity<CartDto> create(@AuthenticationPrincipal User user, @Valid @RequestBody CartDto request) {
        return new ResponseEntity<>(cartService.add(user, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto> update(@PathVariable UUID id, @AuthenticationPrincipal User user, @Valid @RequestBody CartDto request) {
        return new ResponseEntity<>(cartService.update(id, user, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        cartService.delete(id, user);
        return ResponseEntity.ok().build();
    }
}
