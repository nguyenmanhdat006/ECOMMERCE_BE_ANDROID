package com.nguyendat.shopee_be.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String movementType; // IN, OUT, ADJUSTMENT
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Integer previousStock;
    
    @Column(nullable = false)
    private Integer newStock;
    
    @Column(nullable = false)
    private UUID referenceId; // ID của order, receipt, etc.
    
    @Column(nullable = false)
    private String referenceType; // ORDER, RECEIPT, ADJUSTMENT
    
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date movementDate;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    @JsonIgnore
    private ProductVariant productVariant;
    
    @PrePersist
    protected void onCreate() {
        movementDate = new Date();
    }
}