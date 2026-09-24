package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.InventoryReceipt;
import com.nguyendat.shopee_be.services.InventoryReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory-receipts")
@CrossOrigin
@Tag(name = "Inventory Receipts", description = "Manage inventory receipts")
public class InventoryReceiptController {

    @Autowired
    private InventoryReceiptService inventoryReceiptService;

    @GetMapping
    @Operation(summary = "Get all inventory receipts")
    public ResponseEntity<List<InventoryReceipt>> getAll() {
        return new ResponseEntity<>(inventoryReceiptService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory receipt by id")
    public ResponseEntity<InventoryReceipt> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(inventoryReceiptService.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create inventory receipt")
    public ResponseEntity<InventoryReceipt> create(@RequestBody InventoryReceipt entity) {
        InventoryReceipt created = inventoryReceiptService.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory receipt")
    public ResponseEntity<InventoryReceipt> update(@PathVariable UUID id, @RequestBody InventoryReceipt entity) {
        InventoryReceipt updated = inventoryReceiptService.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory receipt")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryReceiptService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
