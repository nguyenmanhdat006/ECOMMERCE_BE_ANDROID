package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
	List<Cart> findAllByUser_Id(UUID userId);
}


