package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.CategoryType;
import com.nguyendat.shopee_be.services.CategoryTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nguyendat.shopee_be.dto.CategoryTypeRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category-types")
@CrossOrigin
@Tag(name = "Category Types", description = "Manage category types")
public class CategoryTypeController {

    @Autowired
    private CategoryTypeService categoryTypeService;

    @GetMapping
    @Operation(summary = "Get all category types")
    public ResponseEntity<List<CategoryType>> getAll() {
        return new ResponseEntity<>(categoryTypeService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category type by id")
    public ResponseEntity<CategoryType> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(categoryTypeService.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create category type")
    public ResponseEntity<CategoryType> create(@RequestBody CategoryTypeRequest req) {
        CategoryType created = categoryTypeService.create(req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category type")
    public ResponseEntity<CategoryType> update(
            @PathVariable UUID id,
            @RequestBody CategoryTypeRequest req) {
        CategoryType updated = categoryTypeService.update(id, req);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category type")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryTypeService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
