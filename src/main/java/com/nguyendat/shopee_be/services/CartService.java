package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.dto.CartResponse;
import com.nguyendat.shopee_be.dto.CartResponseDto;

import java.util.List;
import java.util.UUID;

public interface CartService {
    List<CartResponseDto> findAll();
    List<CartResponseDto> findByUserId(UUID userId);
    CartDto findById(UUID id);
    CartDto create(CartDto cartDto);
    CartDto update(UUID id, CartDto cartDto);
    void deleteById(UUID id);
}
