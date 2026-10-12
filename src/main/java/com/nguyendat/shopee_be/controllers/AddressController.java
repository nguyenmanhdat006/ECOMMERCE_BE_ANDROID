package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.dto.AddressResponse;
import com.nguyendat.shopee_be.entities.Address;
import com.nguyendat.shopee_be.services.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import com.nguyendat.shopee_be.ultil.AccessUtil;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin
@Tag(name = "Addresses", description = "Manage user addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @GetMapping
    @Operation(summary = "Get all addresses")
    public ResponseEntity<List<Address>> getAll() {
        return new ResponseEntity<>(addressService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/my-addresses")
    @Operation(summary = "Get addresses of authenticated user", description = "Returns all addresses belonging to the currently authenticated user")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(Principal principal) {
        User currentUser = getCurrentUser(principal);
        List<AddressResponse> addresses = addressService.findByUserAsDto(currentUser);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by id")
    public ResponseEntity<Address> getById(@PathVariable UUID id, Principal principal) {
        Address address = addressService.findById(id);
        AccessUtil.requireOwnerOrAdmin(getCurrentUser(principal), address.getUser().getId());
        return new ResponseEntity<>(address, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create address", description = "Create a new address for the authenticated user")
    public ResponseEntity<Address> create(@RequestBody Address address, Principal principal) {
        User currentUser = getCurrentUser(principal);
        address.setUser(currentUser);
        Address created = addressService.create(address);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address", description = "Update an existing address. Only the owner can update their address")
    public ResponseEntity<Address> update(@PathVariable UUID id, @RequestBody Address address, Principal principal) {
        User currentUser = getCurrentUser(principal);
        
        // Verify that the address belongs to the current user
        Address existingAddress = addressService.findById(id);
        if (!existingAddress.getUser().getId().equals(currentUser.getId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        address.setUser(currentUser);
        Address updated = addressService.update(id, address);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address", description = "Delete an address. Only the owner can delete their address")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        User currentUser = getCurrentUser(principal);
        
        // Verify that the address belongs to the current user
        Address existingAddress = addressService.findById(id);
        if (!existingAddress.getUser().getId().equals(currentUser.getId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        addressService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get addresses by user id", description = "Returns all addresses belonging to the given user id")
    public ResponseEntity<List<AddressResponse>> getByUserId(@PathVariable UUID userId, Principal principal) {
        AccessUtil.requireOwnerOrAdmin(getCurrentUser(principal), userId);
        Optional<User> userOpt = userDetailRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<AddressResponse> addresses = addressService.findByUserAsDto(userOpt.get());
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        return (User) userDetailsService.loadUserByUsername(principal.getName());
    }
}
