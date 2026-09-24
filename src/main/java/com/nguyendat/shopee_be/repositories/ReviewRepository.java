package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserIdAndOrderItemId(UUID userId, UUID orderItemId);

    List<Review> findByProductId(UUID productId);
}
