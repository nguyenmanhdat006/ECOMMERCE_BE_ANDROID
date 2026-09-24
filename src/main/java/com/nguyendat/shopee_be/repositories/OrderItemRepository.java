package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.entities.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    // Top sold products (product id, product name, total quantity sold) — aggregate across all order_items
    @Query(value = "SELECT oi.product_id AS productId, p.name AS productName, COALESCE(SUM(oi.quantity),0) AS total_sold " +
	    "FROM order_items oi " +
	    "JOIN products p ON p.id = oi.product_id " +
	    "GROUP BY oi.product_id, p.name " +
	    "ORDER BY total_sold DESC", nativeQuery = true)
    List<Object[]> findTopProductsByQuantity();

	// Count order items that belong to orders with given status (diagnostic)
	long countByOrderStatus(@Param("status") OrderStatus status);

	// Fetch sample order items for given order status (diagnostic)
	List<OrderItem> findByOrderStatus(@Param("status") OrderStatus status, Pageable pageable);

	// Find all order items for a given customer where isReviewed = false
	@Query("SELECT oi FROM OrderItem oi WHERE oi.order.customer.id = :customerId AND (oi.isReviewed = false OR oi.isReviewed IS NULL)")
	List<OrderItem> findUnreviewedByCustomerId(@Param("customerId") java.util.UUID customerId);
}


