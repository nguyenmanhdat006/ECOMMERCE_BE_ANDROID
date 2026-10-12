package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.InventoryReceiptRequest;
import com.nguyendat.shopee_be.dto.InventoryReceiptResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryReceiptService {

    List<InventoryReceiptResponse> findAll();

    InventoryReceiptResponse findById(UUID id);

    /** Tạo phiếu; nếu status = APPROVED thì cộng tồn kho ngay. */
    InventoryReceiptResponse create(InventoryReceiptRequest request);

    /**
     * Sửa phiếu đang PENDING (nhà cung cấp, ghi chú, dòng chi tiết) hoặc chuyển trạng thái:
     * PENDING -> APPROVED (cộng kho) hoặc PENDING -> CANCELLED. APPROVED và CANCELLED là trạng thái cuối.
     */
    InventoryReceiptResponse update(UUID id, InventoryReceiptRequest request);

    /** Chỉ xóa được phiếu PENDING hoặc CANCELLED. */
    void deleteById(UUID id);
}
