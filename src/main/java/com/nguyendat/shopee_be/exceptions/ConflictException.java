package com.nguyendat.shopee_be.exceptions;

// Xung đột dữ liệu: trùng mã, hết hàng, đối tượng đang được tham chiếu... -> HTTP 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
