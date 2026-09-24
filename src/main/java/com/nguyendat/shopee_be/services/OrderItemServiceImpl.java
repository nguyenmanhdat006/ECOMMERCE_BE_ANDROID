package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.OrderItemRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository repository;

    public OrderItemServiceImpl(OrderItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OrderItem> findAll() {
        return repository.findAll();
    }

    @Override
    public OrderItem findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("OrderItem not found with id: " + id));
    }

    @Override
    public OrderItem create(OrderItem entity) {
        return repository.save(entity);
    }

    @Override
    public OrderItem update(UUID id, OrderItem entity) {
        OrderItem existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("OrderItem not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
