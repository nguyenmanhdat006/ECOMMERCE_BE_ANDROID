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
public class CartDto {
    private UUID id;
    private Integer quantity;
    private Date createdAt;
    private Date updatedAt;
    private UUID userId;
    private UUID productId;
    private UUID productVariantId;
}
