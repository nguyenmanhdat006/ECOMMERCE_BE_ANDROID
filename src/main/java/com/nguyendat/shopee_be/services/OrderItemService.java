package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.OrderItem;
import java.util.List;
import java.util.UUID;

public interface OrderItemService {
    List<OrderItem> findAll();
    OrderItem findById(UUID id);
    OrderItem create(OrderItem entity);
    OrderItem update(UUID id, OrderItem entity);
    void deleteById(UUID id);
}
