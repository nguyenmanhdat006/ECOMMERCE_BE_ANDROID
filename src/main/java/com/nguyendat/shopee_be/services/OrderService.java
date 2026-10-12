package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.OrderRequest;
import com.nguyendat.shopee_be.entities.Order;
import com.nguyendat.shopee_be.entities.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    List<Order> findAll();
    Order findById(UUID id);
    Order create(OrderRequest request, com.nguyendat.shopee_be.entities.User customer);
    void deleteById(UUID id);

    // Thanh toán VNPay
    String createVnpayUrl(UUID orderId, com.nguyendat.shopee_be.entities.User requester);
    boolean processVnpayReturned(Map<String, String> params);

    // Cập nhật trạng thái đơn hàng
    /**
     * @param orderId ID đơn hàng
     * @param newStatus trạng thái mới (Enum OrderStatus)
     * @param changedBy username hoặc role của người thao tác
     */
    Order updateStatus(UUID orderId, OrderStatus newStatus, String changedBy);

    /**
     * Cập nhật trạng thái thanh toán của đơn hàng
     * @param orderId ID đơn hàng
     * @param newStatus trạng thái thanh toán (PaymentStatus enum)
     * @param changedBy người thực hiện (username/role)
     */
    Order updatePaymentStatus(UUID orderId, com.nguyendat.shopee_be.entities.PaymentStatus newStatus, String changedBy);

    // Lấy đơn hàng theo user (dùng khi client truyền access token)
    java.util.List<Order> findByUser(com.nguyendat.shopee_be.entities.User user);

    // Lấy các order item chưa được review cho user (customer)
    java.util.List<com.nguyendat.shopee_be.entities.OrderItem> findUnreviewedOrderItemsByUser(com.nguyendat.shopee_be.entities.User user);
}
