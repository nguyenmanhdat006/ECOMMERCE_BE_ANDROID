package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.ProductVariantRequest;
import com.nguyendat.shopee_be.dto.ProductVariantResponse;
import com.nguyendat.shopee_be.services.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<ProductVariantResponse>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product variant by id")
    public ResponseEntity<ProductVariantResponse> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create product variant for an existing product (ADMIN)")
    public ResponseEntity<ProductVariantResponse> create(@Valid @RequestBody ProductVariantRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product variant (ADMIN)")
    public ResponseEntity<ProductVariantResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductVariantRequest request) {
        return new ResponseEntity<>(service.update(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product variant that has no orders, receipts or stock movements (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
