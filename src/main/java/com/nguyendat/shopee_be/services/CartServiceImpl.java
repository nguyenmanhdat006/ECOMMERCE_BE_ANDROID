package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.CartDto;
import com.nguyendat.shopee_be.dto.CartResponseDto;
import com.nguyendat.shopee_be.dto.ProductDto;
import com.nguyendat.shopee_be.dto.ProductVariantDto;
import com.nguyendat.shopee_be.entities.Cart;
import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.mapper.CartMapper;
import com.nguyendat.shopee_be.repositories.CartRepository;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import com.nguyendat.shopee_be.ultil.AccessUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository repository;
    private final CartMapper cartMapper;
    private final ProductVariantRepository variantRepository;

    public CartServiceImpl(CartRepository repository, CartMapper cartMapper, ProductVariantRepository variantRepository) {
        this.repository = repository;
        this.cartMapper = cartMapper;
        this.variantRepository = variantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponseDto> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponseDto> findByUserId(UUID userId) {
        return repository.findAllByUser_Id(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto findById(UUID id) {
        return cartMapper.toDto(find(id));
    }

    @Override
    @Transactional
    public CartDto add(User user, CartDto request) {
        ProductVariant variant = variantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundEx("Product variant not found with id: " + request.getProductVariantId()));
        if (request.getProductId() != null && !request.getProductId().equals(variant.getProduct().getId())) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm " + request.getProductId());
        }

        Cart cart = repository.findByUser_IdAndProductVariant_Id(user.getId(), variant.getId()).orElse(null);
        int wanted = request.getQuantity() + (cart != null ? cart.getQuantity() : 0);
        checkStock(variant, wanted);

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setProduct(variant.getProduct());
            cart.setProductVariant(variant);
        }
        cart.setQuantity(wanted);
        return cartMapper.toDto(repository.save(cart));
    }

    @Override
    @Transactional
    public CartDto update(UUID id, User actor, CartDto request) {
        Cart cart = find(id);
        AccessUtil.requireOwnerOrAdmin(actor, cart.getUser().getId());
        checkStock(cart.getProductVariant(), request.getQuantity());
        cart.setQuantity(request.getQuantity());
        return cartMapper.toDto(repository.save(cart));
    }

    @Override
    @Transactional
    public void delete(UUID id, User actor) {
        Cart cart = find(id);
        AccessUtil.requireOwnerOrAdmin(actor, cart.getUser().getId());
        repository.delete(cart);
    }

    private Cart find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Cart not found with id: " + id));
    }

    // Số lượng trong giỏ không được vượt tồn kho hiện tại
    private void checkStock(ProductVariant variant, int quantity) {
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (quantity > stock) {
            throw new IllegalArgumentException("Số lượng vượt quá tồn kho (hiện còn " + stock + ")");
        }
    }

    private CartResponseDto toResponse(Cart cart) {
        ProductDto productDto = ProductDto.builder()
                .id(cart.getProduct().getId())
                .name(cart.getProduct().getName())
                .price(cart.getProduct().getPrice())
                .thumbnail(cart.getProduct().getResources().isEmpty() ? null : cart.getProduct().getResources().get(0).getUrl())
                .build();
        ProductVariantDto variantDto = null;
        if (cart.getProductVariant() != null) {
            variantDto = ProductVariantDto.builder()
                    .id(cart.getProductVariant().getId())
                    .color(cart.getProductVariant().getColor())
                    .size(cart.getProductVariant().getSize())
                    .stockQuantity(cart.getProductVariant().getStockQuantity())
                    .build();
        }
        return CartResponseDto.builder()
                .id(cart.getId())
                .product(productDto)
                .productVariant(variantDto)
                .quantity(cart.getQuantity())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .userId(cart.getUser().getId())
                .build();
    }
}
