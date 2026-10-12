package com.nguyendat.shopee_be.ultil;

import com.nguyendat.shopee_be.entities.Role;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.exceptions.ForbiddenException;

import java.util.UUID;

// Kiểm tra quyền sở hữu dữ liệu: chủ sở hữu hoặc ADMIN mới được thao tác.
public final class AccessUtil {

    private AccessUtil() {
    }

    public static boolean isAdmin(User user) {
        return user != null && user.hasRole(Role.ADMIN);
    }

    public static boolean isOwnerOrAdmin(User actor, UUID ownerId) {
        return actor != null && (isAdmin(actor) || (ownerId != null && ownerId.equals(actor.getId())));
    }

    public static void requireOwnerOrAdmin(User actor, UUID ownerId) {
        if (!isOwnerOrAdmin(actor, ownerId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập dữ liệu này");
        }
    }
}
