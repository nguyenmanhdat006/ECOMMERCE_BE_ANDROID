package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.CategoryType;
import com.nguyendat.shopee_be.dto.CategoryTypeRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryTypeService {
    List<CategoryType> findAll();
    CategoryType findById(UUID id);
    CategoryType create(CategoryTypeRequest request);
    CategoryType update(UUID id, CategoryTypeRequest request);
    void deleteById(UUID id);
}
