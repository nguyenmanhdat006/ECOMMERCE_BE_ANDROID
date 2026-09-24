package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.StockMovement;
import java.util.List;
import java.util.UUID;

public interface StockMovementService {
    List<StockMovement> findAll();
    StockMovement findById(UUID id);
    StockMovement create(StockMovement entity);
    StockMovement update(UUID id, StockMovement entity);
    void deleteById(UUID id);
}
