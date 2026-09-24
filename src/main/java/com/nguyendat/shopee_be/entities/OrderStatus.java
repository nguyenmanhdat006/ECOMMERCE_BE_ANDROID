package com.nguyendat.shopee_be.entities;

public enum OrderStatus {
    PENDING,       // Chờ xác nhận
    SHIPPING,      // Đang vận chuyển
    WAIT_DELIVER,  // Chờ giao hàng
    PAID,          // Hoàn thành
    CANCELED,      // Đã hủy
    REFUND         // Trả hàng / Hoàn tiền
}