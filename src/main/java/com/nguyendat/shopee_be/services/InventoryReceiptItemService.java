package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.InventoryReceiptItem;
import java.util.List;
import java.util.UUID;

public interface InventoryReceiptItemService {
    List<InventoryReceiptItem> findAll();
    InventoryReceiptItem findById(UUID id);
    InventoryReceiptItem create(InventoryReceiptItem entity);
    InventoryReceiptItem update(UUID id, InventoryReceiptItem entity);
    void deleteById(UUID id);
}
