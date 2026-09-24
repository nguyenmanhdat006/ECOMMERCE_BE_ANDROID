package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.PurchaseHistory;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.PurchaseHistoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PurchaseHistoryServiceImpl implements PurchaseHistoryService {
    private final PurchaseHistoryRepository repository;

    public PurchaseHistoryServiceImpl(PurchaseHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PurchaseHistory> findAll() {
        return repository.findAll();
    }

    @Override
    public PurchaseHistory findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("PurchaseHistory not found with id: " + id));
    }

    @Override
    public PurchaseHistory create(PurchaseHistory entity) {
        return repository.save(entity);
    }

    @Override
    public PurchaseHistory update(UUID id, PurchaseHistory entity) {
        PurchaseHistory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("PurchaseHistory not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
