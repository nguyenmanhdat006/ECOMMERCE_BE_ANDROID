package com.nguyendat.shopee_be.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceipt {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String receiptNumber;
    
    @Column(nullable = false)
    private String supplierName;
    
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date receiptDate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(nullable = false)
    private String status; // PENDING, APPROVED, CANCELLED
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "inventoryReceipt", cascade = CascadeType.ALL)
    private List<InventoryReceiptItem> inventoryReceiptItems;
    
    @PrePersist
    protected void onCreate() {
        receiptDate = new Date();
    }
}