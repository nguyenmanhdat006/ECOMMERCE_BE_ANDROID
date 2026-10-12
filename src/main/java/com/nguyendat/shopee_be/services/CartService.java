package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.dto.CartResponseDto;
import com.nguyendat.shopee_be.entities.User;

import java.util.List;
import java.util.UUID;

public interface CartService {

    /** Tất cả dòng giỏ hàng (ADMIN). */
    List<CartResponseDto> findAll();

    List<CartResponseDto> findByUserId(UUID userId);

    CartDto findById(UUID id);

    /** Thêm vào giỏ cho người dùng hiện tại; đã có cùng biến thể thì cộng dồn số lượng. */
    CartDto add(User user, CartDto request);

    /** Đặt lại số lượng của một dòng giỏ (chủ giỏ hoặc ADMIN). */
    CartDto update(UUID id, User actor, CartDto request);

    void delete(UUID id, User actor);
}
