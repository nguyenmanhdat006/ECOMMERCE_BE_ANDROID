package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.Resources;
import java.util.List;
import java.util.UUID;

public interface ResourcesService {
    List<Resources> findAll();
    Resources findById(UUID id);
    Resources create(Resources entity);
    Resources update(UUID id, Resources entity);
    void deleteById(UUID id);
}
