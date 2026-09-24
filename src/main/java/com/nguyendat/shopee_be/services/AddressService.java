package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.dto.AddressResponse;
import com.nguyendat.shopee_be.entities.Address;
import java.util.List;
import java.util.UUID;

public interface AddressService {
    List<Address> findAll();
    Address findById(UUID id);
    Address create(Address address);
    Address update(UUID id, Address address);
    void deleteById(UUID id);
    List<Address> findByUser(User user);
    List<AddressResponse> findByUserAsDto(User user);
}
