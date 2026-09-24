package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.ProductStatus;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.ProductStatusRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductStatusServiceImpl implements ProductStatusService {
    private final ProductStatusRepository repository;

    public ProductStatusServiceImpl(ProductStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductStatus> findAll() {
        return repository.findAll();
    }

    @Override
    public ProductStatus findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("ProductStatus not found with id: " + id));
    }

    @Override
    public ProductStatus create(ProductStatus entity) {
        return repository.save(entity);
    }

    @Override
    public ProductStatus update(UUID id, ProductStatus entity) {
        ProductStatus existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("ProductStatus not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
