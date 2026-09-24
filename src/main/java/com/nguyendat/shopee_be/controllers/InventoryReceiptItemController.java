package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.InventoryReceiptItem;
import com.nguyendat.shopee_be.services.InventoryReceiptItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory-receipt-items")
@CrossOrigin
@Tag(name = "Inventory Receipt Items", description = "Manage items on inventory receipts")
public class InventoryReceiptItemController {

    @Autowired
    private InventoryReceiptItemService service;

    @GetMapping
    @Operation(summary = "Get all inventory receipt items")
    public ResponseEntity<List<InventoryReceiptItem>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory receipt item by id")
    public ResponseEntity<InventoryReceiptItem> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create inventory receipt item")
    public ResponseEntity<InventoryReceiptItem> create(@RequestBody InventoryReceiptItem entity) {
        InventoryReceiptItem created = service.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory receipt item")
    public ResponseEntity<InventoryReceiptItem> update(@PathVariable UUID id, @RequestBody InventoryReceiptItem entity) {
        InventoryReceiptItem updated = service.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory receipt item")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
