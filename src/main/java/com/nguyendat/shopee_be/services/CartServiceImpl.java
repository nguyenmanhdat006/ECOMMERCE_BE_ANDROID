package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.entities.Cart;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.mapper.CartMapper;
import com.nguyendat.shopee_be.repositories.CartRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.nguyendat.shopee_be.dto.CartResponseDto;
import com.nguyendat.shopee_be.dto.ProductDto;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository repository;
    private final CartMapper cartMapper;

    public CartServiceImpl(CartRepository repository, CartMapper cartMapper) {
        this.repository = repository;
        this.cartMapper = cartMapper;
    }

    @Override
    public List<CartResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(cart -> {
                    ProductDto productDto = ProductDto.builder()
                            .id(cart.getProduct().getId())
                            .name(cart.getProduct().getName())
                            .price(cart.getProduct().getPrice())
                            .thumbnail(cart.getProduct().getResources().isEmpty() ? null : cart.getProduct().getResources().get(0).getUrl())
                            .build();
            com.nguyendat.shopee_be.dto.ProductVariantDto productVariantDto = null;
            if (cart.getProductVariant() != null) {
            productVariantDto = com.nguyendat.shopee_be.dto.ProductVariantDto.builder()
                .id(cart.getProductVariant().getId())
                .color(cart.getProductVariant().getColor())
                .size(cart.getProductVariant().getSize())
                .stockQuantity(cart.getProductVariant().getStockQuantity())
                .build();
            }

                    return CartResponseDto.builder()
                            .id(cart.getId())
                            .product(productDto)
                .productVariant(productVariantDto)
                            .quantity(cart.getQuantity())       
                            .createdAt(cart.getCreatedAt())
                            .updatedAt(cart.getUpdatedAt())
                            .userId(cart.getUser().getId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CartResponseDto> findByUserId(UUID userId) {
        return repository.findAllByUser_Id(userId)
                .stream()
                .map(cart -> {
                    ProductDto productDto = ProductDto.builder()
                            .id(cart.getProduct().getId())
                            .name(cart.getProduct().getName())
                            .price(cart.getProduct().getPrice())
                            .thumbnail(cart.getProduct().getResources().isEmpty() ? null : cart.getProduct().getResources().get(0).getUrl())
                            .build();
            com.nguyendat.shopee_be.dto.ProductVariantDto productVariantDto = null;
            if (cart.getProductVariant() != null) {
            productVariantDto = com.nguyendat.shopee_be.dto.ProductVariantDto.builder()
                .id(cart.getProductVariant().getId())
                .color(cart.getProductVariant().getColor())
                .size(cart.getProductVariant().getSize())
                .stockQuantity(cart.getProductVariant().getStockQuantity())
                .build();
            }

                    return CartResponseDto.builder()
                            .id(cart.getId())
                            .product(productDto)
                .productVariant(productVariantDto)
                            .quantity(cart.getQuantity())       
                            .createdAt(cart.getCreatedAt())
                            .updatedAt(cart.getUpdatedAt())
                            .userId(cart.getUser().getId())
                            .build();
                })
                .collect(Collectors.toList());
    }


    @Override
    public CartDto findById(UUID id) {
        Cart cart = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Cart not found with id: " + id));
        return cartMapper.toDto(cart);
    }

    @Override
    public CartDto create(CartDto cartDto) {
        Cart cart = cartMapper.toEntity(cartDto);
        Cart saved = repository.save(cart);
        return cartMapper.toDto(saved);
    }

    @Override
    public CartDto update(UUID id, CartDto cartDto) {
        Cart existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Cart not found with id: " + id));
        Cart updatedEntity = cartMapper.toEntity(cartDto);
        // copy properties except id and relations if updatedEntity has nulls
        BeanUtils.copyProperties(updatedEntity, existing, "id");
        Cart saved = repository.save(existing);
        return cartMapper.toDto(saved);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
