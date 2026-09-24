package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.StockMovement;
import com.nguyendat.shopee_be.services.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock-movements")
@CrossOrigin
@Tag(name = "Stock Movements", description = "Track stock movements (in/out) for products")
public class StockMovementController {

    @Autowired
    private StockMovementService service;

    @GetMapping
    @Operation(summary = "Get all stock movements")
    public ResponseEntity<List<StockMovement>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock movement by id")
    public ResponseEntity<StockMovement> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create stock movement record")
    public ResponseEntity<StockMovement> create(@RequestBody StockMovement entity) {
        StockMovement created = service.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update stock movement record")
    public ResponseEntity<StockMovement> update(@PathVariable UUID id, @RequestBody StockMovement entity) {
        StockMovement updated = service.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete stock movement record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
