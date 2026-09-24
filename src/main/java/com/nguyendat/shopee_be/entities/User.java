package com.nguyendat.shopee_be.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Table(name = "AUTH_USER_DETAILS")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private UUID id;

    private String firstName;
    private String lastName;

    @JsonIgnore
    private String password;

    private Date createdOn;
    private Date updatedOn;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;
    private String provider;
    private String verificationCode;
    @Builder.Default
    private boolean enabled = false;

    @Column(nullable = true)
    private String avatar;

    @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(
            name = "auth_user_authority",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"), 
            inverseJoinColumns = @JoinColumn(name = "authorities_id", referencedColumnName = "id"))
    @Builder.Default
    @ToString.Exclude
    private List<Authority> authorities = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Address> addressList;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Cart> carts;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<PurchaseHistory> purchaseHistories;

    @OneToMany(mappedBy = "changedBy", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ProductStatus> productStatusChanges;

    // Danh sách role gốc lưu trong DB
    @JsonIgnore
    public List<Authority> getRoles() {
        return authorities;
    }

    // Role chính để trả về cho FE / nhúng vào JWT (USER hoặc ADMIN)
    @JsonIgnore
    public String getPrimaryRole() {
        if (authorities == null) {
            return Role.USER;
        }
        List<String> roles = authorities.stream().map(a -> Role.normalize(a.getRoleCode())).toList();
        return roles.contains(Role.ADMIN) ? Role.ADMIN : Role.USER;
    }

    public boolean hasRole(String role) {
        return authorities != null && authorities.stream()
                .anyMatch(a -> role.equals(Role.normalize(a.getRoleCode())));
    }

    // Spring Security dùng tiền tố ROLE_ cho hasRole(...)
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .map(a -> new SimpleGrantedAuthority("ROLE_" + Role.normalize(a.getRoleCode())))
                .toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}