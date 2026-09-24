package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.ProductStatus;
import java.util.List;
import java.util.UUID;

public interface ProductStatusService {
    List<ProductStatus> findAll();
    ProductStatus findById(UUID id);
    ProductStatus create(ProductStatus entity);
    ProductStatus update(UUID id, ProductStatus entity);
    void deleteById(UUID id);
}
