package com.nguyendat.shopee_be.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Ngày nhập và tổng tiền do máy chủ tính. status: PENDING (mặc định) hoặc APPROVED (cộng kho ngay).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptRequest {

    @NotBlank(message = "Số phiếu là bắt buộc")
    private String receiptNumber;

    @NotBlank(message = "Nhà cung cấp là bắt buộc")
    private String supplierName;

    private String status;

    private String notes;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất một dòng")
    @Valid
    private List<InventoryReceiptItemRequest> items;
}
