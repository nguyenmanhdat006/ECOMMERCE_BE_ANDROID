package com.nguyendat.shopee_be.repositories;

import com.nguyendat.shopee_be.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDetailRepository extends JpaRepository<User, UUID> {
    User findByEmail(String username);
    Optional<User> findById(UUID id);
}