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
public class StockMovementRequest {
    private String movementType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private UUID referenceId;
    private String referenceType;
    private String notes;
    private UUID productId;
    private UUID productVariantId;
}
