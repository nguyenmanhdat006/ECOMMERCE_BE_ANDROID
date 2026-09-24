package com.nguyendat.shopee_be.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_variant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String color;
    
    @Column(nullable = false)
    private String size;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
        
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
    
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL)
    private List<Cart> cartItems;
    
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL)
    private List<InventoryReceiptItem> inventoryReceiptItems;
    
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL)
    private List<StockMovement> stockMovements;
}