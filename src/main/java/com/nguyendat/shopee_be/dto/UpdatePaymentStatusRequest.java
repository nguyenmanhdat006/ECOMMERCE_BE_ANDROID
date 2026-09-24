package com.nguyendat.shopee_be.dto;

import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {
    private String status; // Should match PaymentStatus enum names
}
