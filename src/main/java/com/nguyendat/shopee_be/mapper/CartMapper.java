package com.nguyendat.shopee_be.mapper;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.dto.CartRequest;
import com.nguyendat.shopee_be.dto.CartResponse;
import com.nguyendat.shopee_be.entities.Cart;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CartMapper {

    private final UserDetailRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartMapper(UserDetailRepository userRepository, ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public CartResponse toResponse(Cart cart) {
        if (cart == null) return null;
        return CartResponse.builder()
                .id(cart.getId())
                .quantity(cart.getQuantity())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .productId(cart.getProduct() != null ? cart.getProduct().getId() : null)
                .productVariantId(cart.getProductVariant() != null ? cart.getProductVariant().getId() : null)
                .build();
    }

    public Cart toEntity(CartRequest request) {
        if (request == null) return null;
        Cart cart = new Cart();
        cart.setQuantity(request.getQuantity());

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            cart.setUser(user);
        }

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId()).orElse(null);
            cart.setProduct(product);
        }

        if (request.getProductVariantId() != null) {
            ProductVariant pv = productVariantRepository.findById(request.getProductVariantId()).orElse(null);
            cart.setProductVariant(pv);
        }

        return cart;
    }

    public Cart toEntity(CartDto dto) {
        if (dto == null) return null;
        Cart cart = new Cart();
        cart.setId(dto.getId());
        cart.setQuantity(dto.getQuantity());

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId()).orElse(null);
            cart.setUser(user);
        }

        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId()).orElse(null);
            cart.setProduct(product);
        }

        if (dto.getProductVariantId() != null) {
            ProductVariant pv = productVariantRepository.findById(dto.getProductVariantId()).orElse(null);
            cart.setProductVariant(pv);
        }

        return cart;
    }

    public CartDto toDto(Cart cart) {
        if (cart == null) return null;
        return CartDto.builder()
                .id(cart.getId())
                .quantity(cart.getQuantity())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .productId(cart.getProduct() != null ? cart.getProduct().getId() : null)
                .productVariantId(cart.getProductVariant() != null ? cart.getProductVariant().getId() : null)
                .build();
    }
}
