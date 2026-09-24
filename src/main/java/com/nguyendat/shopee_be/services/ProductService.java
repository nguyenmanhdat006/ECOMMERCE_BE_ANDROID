package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ProductDto;
import com.nguyendat.shopee_be.entities.Product;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


public interface ProductService {

    public Product addProduct(ProductDto product);
    public List<ProductDto> getAllProducts(UUID categoryId, UUID typeId);

    ProductDto getProductBySlug(String slug);

    ProductDto getProductById(UUID id);

    Product updateProduct(ProductDto productDto, UUID id);

    Product fetchProductById(UUID uuid) throws Exception;
    
    Page<ProductDto> searchProducts(
        String keyword,
        UUID categoryId,
        UUID typeId,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Float minRating,
        Boolean isNewArrival,
        String sortBy,
        String sortDirection,
        int page,
        int size
    );

    void deleteProductById(UUID id);
}