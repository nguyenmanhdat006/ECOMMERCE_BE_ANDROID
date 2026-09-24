package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.InventoryReceiptItem;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.InventoryReceiptItemRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryReceiptItemServiceImpl implements InventoryReceiptItemService {
    private final InventoryReceiptItemRepository repository;

    public InventoryReceiptItemServiceImpl(InventoryReceiptItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InventoryReceiptItem> findAll() {
        return repository.findAll();
    }

    @Override
    public InventoryReceiptItem findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("InventoryReceiptItem not found with id: " + id));
    }

    @Override
    public InventoryReceiptItem create(InventoryReceiptItem entity) {
        return repository.save(entity);
    }

    @Override
    public InventoryReceiptItem update(UUID id, InventoryReceiptItem entity) {
        InventoryReceiptItem existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("InventoryReceiptItem not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
