package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.services.CartService;
import com.nguyendat.shopee_be.dto.CartDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.nguyendat.shopee_be.dto.CartResponseDto;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin
@Tag(name = "Carts", description = "Manage shopping carts")
public class CartController {

    @Autowired
    private CartService cartService;


    @GetMapping
    public ResponseEntity<List<CartResponseDto>> getAll() {
        List<CartResponseDto> dtos = cartService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartResponseDto>> getByUserId(@PathVariable UUID userId) {
        List<CartResponseDto> dtos = cartService.findByUserId(userId);
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto> getById(@PathVariable UUID id) {
        CartDto dto = cartService.findById(id);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<CartDto> create(@RequestBody CartDto request) {
        CartDto created = cartService.create(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto> update(@PathVariable UUID id, @RequestBody CartDto request) {
        CartDto updated = cartService.update(id, request);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cartService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
