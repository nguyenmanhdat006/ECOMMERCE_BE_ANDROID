package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository repository;

    public ProductVariantServiceImpl(ProductVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductVariant> findAll() {
        return repository.findAll();
    }

    @Override
    public ProductVariant findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("ProductVariant not found with id: " + id));
    }

    @Override
    public ProductVariant create(ProductVariant entity) {
        return repository.save(entity);
    }

    @Override
    public ProductVariant update(UUID id, ProductVariant entity) {
        ProductVariant existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("ProductVariant not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
