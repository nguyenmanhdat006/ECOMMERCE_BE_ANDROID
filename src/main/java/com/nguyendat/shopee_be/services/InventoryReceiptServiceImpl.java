package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.InventoryReceipt;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.InventoryReceiptRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryReceiptServiceImpl implements InventoryReceiptService {
    private final InventoryReceiptRepository repository;

    public InventoryReceiptServiceImpl(InventoryReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InventoryReceipt> findAll() {
        return repository.findAll();
    }

    @Override
    public InventoryReceipt findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("InventoryReceipt not found with id: " + id));
    }

    @Override
    public InventoryReceipt create(InventoryReceipt entity) {
        return repository.save(entity);
    }

    @Override
    public InventoryReceipt update(UUID id, InventoryReceipt entity) {
        InventoryReceipt existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("InventoryReceipt not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
