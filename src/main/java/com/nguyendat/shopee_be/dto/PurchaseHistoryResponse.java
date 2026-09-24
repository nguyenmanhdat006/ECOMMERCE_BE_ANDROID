package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseHistoryResponse {
    private UUID id;
    private Date purchaseDate;
    private BigDecimal totalAmount;
    private String status;
    private UUID customerId;
    private UUID orderId;
}
