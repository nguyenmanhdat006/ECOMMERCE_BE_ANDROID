package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.Resources;
import com.nguyendat.shopee_be.services.ResourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin
@Tag(name = "Resources", description = "Manage product resources (images/files)")
public class ResourcesController {

    @Autowired
    private ResourcesService service;

    @GetMapping
    @Operation(summary = "Get all resources")
    public ResponseEntity<List<Resources>> getAll() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by id")
    public ResponseEntity<Resources> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create resource")
    public ResponseEntity<Resources> create(@RequestBody Resources entity) {
        Resources created = service.create(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update resource")
    public ResponseEntity<Resources> update(@PathVariable UUID id, @RequestBody Resources entity) {
        Resources updated = service.update(id, entity);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete resource")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
