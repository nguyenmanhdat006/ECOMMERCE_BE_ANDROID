package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private UUID id;
    private Integer quantity;
    private Date createdAt;
    private Date updatedAt;
    private UUID userId;
    private UUID productId;
    private UUID productVariantId;
}
