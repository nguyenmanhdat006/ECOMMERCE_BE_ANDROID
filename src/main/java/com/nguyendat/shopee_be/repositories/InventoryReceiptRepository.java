package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, UUID> {
}

