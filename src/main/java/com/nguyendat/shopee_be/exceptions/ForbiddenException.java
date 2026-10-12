package com.nguyendat.shopee_be.exceptions;

// Người dùng đã đăng nhập nhưng không được phép thao tác trên dữ liệu này -> HTTP 403
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
