package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ProductVariantRequest;
import com.nguyendat.shopee_be.dto.ProductVariantResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {

    List<ProductVariantResponse> findAll();

    ProductVariantResponse findById(UUID id);

    /** Thêm biến thể cho một sản phẩm đã có. */
    ProductVariantResponse create(ProductVariantRequest request);

    /** Sửa màu, kích cỡ, tồn kho (không đổi sản phẩm). */
    ProductVariantResponse update(UUID id, ProductVariantRequest request);

    /** Xóa biến thể chưa từng phát sinh đơn hàng, phiếu nhập hoặc biến động kho. */
    void deleteById(UUID id);
}
