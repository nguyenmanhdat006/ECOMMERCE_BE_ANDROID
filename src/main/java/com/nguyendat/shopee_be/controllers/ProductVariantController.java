package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.services.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-variants")
@CrossOrigin
@Tag(name = "Product Variants", description = "Manage product variants")
public class ProductVariantController {

    @Autowired
    private ProductVariantService service;

    @GetMapping
    @Operation(summary = "Get all product variants")
    public ResponseEntity<List<ProductVariant>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product variant by id")
    public ResponseEntity<ProductVariant> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create product variant")
    public ResponseEntity<ProductVariant> create(@RequestBody ProductVariant entity) {
        ProductVariant created = service.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product variant")
    public ResponseEntity<ProductVariant> update(@PathVariable UUID id, @RequestBody ProductVariant entity) {
        ProductVariant updated = service.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product variant")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
