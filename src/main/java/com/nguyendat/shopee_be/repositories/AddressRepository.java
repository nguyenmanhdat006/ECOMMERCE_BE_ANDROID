package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUser(User user);
}
