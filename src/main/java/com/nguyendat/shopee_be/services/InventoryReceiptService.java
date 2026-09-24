package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.InventoryReceipt;
import java.util.List;
import java.util.UUID;

public interface InventoryReceiptService {
    List<InventoryReceipt> findAll();
    InventoryReceipt findById(UUID id);
    InventoryReceipt create(InventoryReceipt entity);
    InventoryReceipt update(UUID id, InventoryReceipt entity);
    void deleteById(UUID id);
}
