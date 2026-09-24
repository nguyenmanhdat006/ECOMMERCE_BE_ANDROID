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
public class ProductStatusResponse {
    private UUID id;
    private Boolean isActive;
    private Boolean isVisible;
    private String reason;
    private Date changedAt;
    private UUID productId;
    private UUID changedById;
}
