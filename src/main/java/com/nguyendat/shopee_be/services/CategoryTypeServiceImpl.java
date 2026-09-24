package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.CategoryType;
import com.nguyendat.shopee_be.dto.CategoryTypeRequest;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.CategoryRepository;
import com.nguyendat.shopee_be.repositories.CategoryTypeRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryTypeServiceImpl implements CategoryTypeService {
    private final CategoryTypeRepository repository;
    private final CategoryRepository categoryRepository;

    public CategoryTypeServiceImpl(CategoryTypeRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryType> findAll() {
        return repository.findAll();
    }

    @Override
    public CategoryType findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("CategoryType not found with id: " + id));
    }

    @Override
    public CategoryType create(CategoryTypeRequest request) {
        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundEx("Category not found"));

        CategoryType entity = CategoryType.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .category(category)
                .build();

        return repository.save(entity);
    }

    @Override
    public CategoryType update(UUID id, CategoryTypeRequest request) {
        CategoryType existing = findById(id);

        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundEx("Category not found"));

        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setDescription(request.getDescription());
        existing.setCategory(category);

        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
