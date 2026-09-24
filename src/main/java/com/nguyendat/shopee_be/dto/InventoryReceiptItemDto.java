package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptItemDto {
    private UUID id;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private UUID receiptId;
    private UUID productId;
    private UUID productVariantId;
}
