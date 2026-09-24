package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.PurchaseHistory;
import java.util.List;
import java.util.UUID;

public interface PurchaseHistoryService {
    List<PurchaseHistory> findAll();
    PurchaseHistory findById(UUID id);
    PurchaseHistory create(PurchaseHistory entity);
    PurchaseHistory update(UUID id, PurchaseHistory entity);
    void deleteById(UUID id);
}
