package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.entities.StockMovement;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import com.nguyendat.shopee_be.repositories.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thay đổi tồn kho của biến thể và ghi sổ biến động kho (stock_movements).
 * Gọi trong một giao dịch của nghiệp vụ gọi tới (đơn hàng, phiếu nhập).
 */
@Service
public class StockService {

    public static final String IN = "IN";
    public static final String OUT = "OUT";

    private final ProductVariantRepository variantRepository;
    private final StockMovementRepository movementRepository;

    public StockService(ProductVariantRepository variantRepository, StockMovementRepository movementRepository) {
        this.variantRepository = variantRepository;
        this.movementRepository = movementRepository;
    }

    /** Khóa và nạp biến thể để thay đổi tồn kho. */
    public ProductVariant lock(UUID variantId) {
        return variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx("Product variant not found with id: " + variantId));
    }

    /** Trừ kho. Không đủ hàng thì báo 409. */
    public void remove(ProductVariant variant, int quantity, UUID referenceId, String referenceType, String note) {
        int current = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (current < quantity) {
            throw new ConflictException("Sản phẩm " + describe(variant) + " chỉ còn " + current + " trong kho, không đủ " + quantity);
        }
        apply(variant, current, current - quantity, quantity, OUT, referenceId, referenceType, note);
    }

    /** Cộng kho. */
    public void add(ProductVariant variant, int quantity, UUID referenceId, String referenceType, String note) {
        int current = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        apply(variant, current, current + quantity, quantity, IN, referenceId, referenceType, note);
    }

    private void apply(ProductVariant variant, int before, int after, int quantity, String type,
                       UUID referenceId, String referenceType, String note) {
        variant.setStockQuantity(after);
        variantRepository.save(variant);
        movementRepository.save(StockMovement.builder()
                .product(variant.getProduct())
                .productVariant(variant)
                .movementType(type)
                .quantity(quantity)
                .previousStock(before)
                .newStock(after)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .notes(note)
                .build());
    }

    private String describe(ProductVariant v) {
        String name = v.getProduct() != null ? v.getProduct().getName() : "";
        return (name + " (" + v.getColor() + ", " + v.getSize() + ")").trim();
    }
}
