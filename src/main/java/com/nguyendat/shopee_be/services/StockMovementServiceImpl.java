package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.StockMovement;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.StockMovementRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StockMovementServiceImpl implements StockMovementService {
    private final StockMovementRepository repository;

    public StockMovementServiceImpl(StockMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StockMovement> findAll() {
        return repository.findAll();
    }

    @Override
    public StockMovement findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("StockMovement not found with id: " + id));
    }

    @Override
    public StockMovement create(StockMovement entity) {
        return repository.save(entity);
    }

    @Override
    public StockMovement update(UUID id, StockMovement entity) {
        StockMovement existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("StockMovement not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
