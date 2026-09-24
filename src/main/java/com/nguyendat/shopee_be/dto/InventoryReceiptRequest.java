package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptRequest {
    private String receiptNumber;
    private String supplierName;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private List<InventoryReceiptItemRequest> items;
}
