package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.Resources;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.ResourcesRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ResourcesServiceImpl implements ResourcesService {
    private final ResourcesRepository repository;

    public ResourcesServiceImpl(ResourcesRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Resources> findAll() {
        return repository.findAll();
    }

    @Override
    public Resources findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Resources not found with id: " + id));
    }

    @Override
    public Resources create(Resources entity) {
        return repository.save(entity);
    }

    @Override
    public Resources update(UUID id, Resources entity) {
        Resources existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Resources not found with id: " + id));
        BeanUtils.copyProperties(entity, existing, "id");
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
