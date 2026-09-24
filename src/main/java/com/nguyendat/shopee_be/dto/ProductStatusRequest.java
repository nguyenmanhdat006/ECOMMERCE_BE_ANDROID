package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatusRequest {
    private Boolean isActive;
    private Boolean isVisible;
    private String reason;
    private UUID productId;
    private UUID changedById;
}
