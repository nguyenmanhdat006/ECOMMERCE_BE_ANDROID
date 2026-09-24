package com.nguyendat.shopee_be.entities;

// Danh sách role của hệ thống. Giá trị lưu trong AUTH_AUTHORITY.roleCode (không có tiền tố ROLE_)
public final class Role {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private Role() {
    }

    // Chuẩn hoá roleCode trong DB (vd: "admin", "ROLE_ADMIN") về dạng "ADMIN"
    public static String normalize(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        String code = roleCode.trim().toUpperCase();
        return code.startsWith("ROLE_") ? code.substring(5) : code;
    }
}
