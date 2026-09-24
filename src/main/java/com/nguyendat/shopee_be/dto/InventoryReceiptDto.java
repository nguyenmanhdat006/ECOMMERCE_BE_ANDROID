package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptDto {
    private UUID id;
    private String receiptNumber;
    private String supplierName;
    private Date receiptDate;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private List<UUID> itemIds;
}
