package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ProductVariantRequest;
import com.nguyendat.shopee_be.dto.ProductVariantResponse;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository repository;
    private final ProductRepository productRepository;

    public ProductVariantServiceImpl(ProductVariantRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse findById(UUID id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public ProductVariantResponse create(ProductVariantRequest request) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("productId là bắt buộc");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundEx("Product Not Found!"));

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setStockQuantity(request.getStockQuantity());
        return toResponse(repository.save(variant));
    }

    @Override
    @Transactional
    public ProductVariantResponse update(UUID id, ProductVariantRequest request) {
        ProductVariant variant = find(id);
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setStockQuantity(request.getStockQuantity());
        return toResponse(repository.save(variant));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        ProductVariant variant = find(id);
        // Quan hệ JPA có cascade nên xóa biến thể đã bán sẽ xóa luôn lịch sử đơn hàng; chặn từ đầu
        boolean used = !variant.getOrderItems().isEmpty()
                || !variant.getInventoryReceiptItems().isEmpty()
                || !variant.getStockMovements().isEmpty();
        if (used) {
            throw new ConflictException("Biến thể đã phát sinh đơn hàng, phiếu nhập hoặc biến động kho nên không thể xóa");
        }
        repository.delete(variant);
    }

    private ProductVariant find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("ProductVariant not found with id: " + id));
    }

    private ProductVariantResponse toResponse(ProductVariant v) {
        return ProductVariantResponse.builder()
                .id(v.getId())
                .productId(v.getProduct() != null ? v.getProduct().getId() : null)
                .color(v.getColor())
                .size(v.getSize())
                .stockQuantity(v.getStockQuantity())
                .build();
    }
}
