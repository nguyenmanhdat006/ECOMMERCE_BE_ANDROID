package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.Order;
import com.nguyendat.shopee_be.entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    boolean existsByOrderNumber(String orderNumber);

    // Đếm đơn theo status
    int countByStatus(OrderStatus status);
    
    // Tính tổng doanh thu TẤT CẢ đơn PAID (all time, không filter ngày)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    double sumTotalRevenueByStatus(@Param("status") OrderStatus status);
    
    // Lấy doanh thu theo giờ HÔM NAY (chỉ đơn PAID)
    @Query("SELECT HOUR(o.orderDate) as hour, COALESCE(SUM(o.totalAmount), 0) as revenue " +
           "FROM Order o " +
           "WHERE o.orderDate >= :startOfDay " +
           "AND o.orderDate < :endOfDay " +
           "AND o.status = :status " +
           "GROUP BY HOUR(o.orderDate) " +
           "ORDER BY HOUR(o.orderDate)")
    List<Object[]> getHourlyRevenue(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay,
        @Param("status") OrderStatus status
    );
    
    // Đếm đơn theo status HÔM NAY (cho bar chart)
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
           "WHERE o.orderDate >= :startOfDay " +
           "AND o.orderDate < :endOfDay " +
           "GROUP BY o.status")
    List<Object[]> countOrdersByStatus(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    // Lấy danh sách đơn hàng theo khách hàng
    List<Order> findByCustomer(com.nguyendat.shopee_be.entities.User customer);

    // Top customers by total spent (only PAID orders). Returns: customer id, customer email, total spent
    @Query("SELECT o.customer.id, o.customer.email, COALESCE(SUM(o.totalAmount),0) " +
        "FROM Order o " +
        "WHERE o.status = :status " +
        "GROUP BY o.customer.id, o.customer.email " +
        "ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopCustomersByTotal(@Param("status") OrderStatus status);

    // Find the most recent order with given status (used for diagnostics/logging)
    Optional<Order> findTopByStatusOrderByOrderDateDesc(OrderStatus status);
}