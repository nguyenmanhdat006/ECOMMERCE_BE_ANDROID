package com.nguyendat.shopee_be.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

// Sản phẩm suy ra từ biến thể; thành tiền do máy chủ tính.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptItemRequest {

    @NotNull(message = "productVariantId là bắt buộc")
    private UUID productVariantId;

    @NotNull(message = "Số lượng là bắt buộc")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Giá vốn là bắt buộc")
    @DecimalMin(value = "0", message = "Giá vốn không được âm")
    private BigDecimal unitCost;
}
