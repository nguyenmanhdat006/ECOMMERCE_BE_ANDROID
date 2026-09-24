package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.services.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin
@Tag(name = "Order Items", description = "Manage items in orders")
public class OrderItemController {

    @Autowired
    private OrderItemService service;

    @GetMapping
    @Operation(summary = "Get all order items")
    public ResponseEntity<List<OrderItem>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order item by id")
    public ResponseEntity<OrderItem> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create order item")
    public ResponseEntity<OrderItem> create(@RequestBody OrderItem entity) {
        OrderItem created = service.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order item")
    public ResponseEntity<OrderItem> update(@PathVariable UUID id, @RequestBody OrderItem entity) {
        OrderItem updated = service.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order item")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
