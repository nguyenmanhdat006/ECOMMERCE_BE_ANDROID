package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.ProductVariant;
import java.util.List;
import java.util.UUID;

public interface ProductVariantService {
    List<ProductVariant> findAll();
    ProductVariant findById(UUID id);
    ProductVariant create(ProductVariant entity);
    ProductVariant update(UUID id, ProductVariant entity);
    void deleteById(UUID id);
}
