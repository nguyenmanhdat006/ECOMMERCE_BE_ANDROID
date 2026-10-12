package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.InventoryReceiptRequest;
import com.nguyendat.shopee_be.dto.InventoryReceiptResponse;
import com.nguyendat.shopee_be.services.InventoryReceiptService;
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
@RequestMapping("/api/inventory-receipts")
@CrossOrigin
@Tag(name = "Inventory Receipts", description = "Manage inventory receipts (ADMIN)")
public class InventoryReceiptController {

    @Autowired
    private InventoryReceiptService inventoryReceiptService;

    @GetMapping
    @Operation(summary = "Get all inventory receipts")
    public ResponseEntity<List<InventoryReceiptResponse>> getAll() {
        return new ResponseEntity<>(inventoryReceiptService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory receipt by id")
    public ResponseEntity<InventoryReceiptResponse> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(inventoryReceiptService.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create inventory receipt (status APPROVED adds stock immediately)")
    public ResponseEntity<InventoryReceiptResponse> create(@Valid @RequestBody InventoryReceiptRequest request) {
        return new ResponseEntity<>(inventoryReceiptService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a PENDING receipt, or approve (adds stock) / cancel it via status")
    public ResponseEntity<InventoryReceiptResponse> update(@PathVariable UUID id, @Valid @RequestBody InventoryReceiptRequest request) {
        return new ResponseEntity<>(inventoryReceiptService.update(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a PENDING or CANCELLED receipt")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryReceiptService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
