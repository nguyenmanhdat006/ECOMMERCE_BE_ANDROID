package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nguyendat.shopee_be.dto.ProductDto;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDto {
    private UUID id;
    private ProductDto product;
    private com.nguyendat.shopee_be.dto.ProductVariantDto productVariant;
    private Integer quantity;
    private Date createdAt;
    private Date updatedAt;
    private UUID userId;
}
