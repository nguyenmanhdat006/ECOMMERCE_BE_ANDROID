package com.nguyendat.shopee_be.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class VnpayRequest {
    private UUID orderId;
    private BigDecimal amount;

    // getters & setters
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
