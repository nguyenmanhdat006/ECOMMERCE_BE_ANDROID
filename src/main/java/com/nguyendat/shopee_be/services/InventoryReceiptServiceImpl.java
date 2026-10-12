package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.InventoryReceiptItemRequest;
import com.nguyendat.shopee_be.dto.InventoryReceiptItemResponse;
import com.nguyendat.shopee_be.dto.InventoryReceiptRequest;
import com.nguyendat.shopee_be.dto.InventoryReceiptResponse;
import com.nguyendat.shopee_be.entities.InventoryReceipt;
import com.nguyendat.shopee_be.entities.InventoryReceiptItem;
import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.InventoryReceiptRepository;
import com.nguyendat.shopee_be.repositories.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryReceiptServiceImpl implements InventoryReceiptService {

    static final String PENDING = "PENDING";
    static final String APPROVED = "APPROVED";
    static final String CANCELLED = "CANCELLED";
    private static final Set<String> STATUSES = Set.of(PENDING, APPROVED, CANCELLED);

    private final InventoryReceiptRepository repository;
    private final ProductVariantRepository variantRepository;
    private final StockService stockService;

    public InventoryReceiptServiceImpl(InventoryReceiptRepository repository, ProductVariantRepository variantRepository,
                                       StockService stockService) {
        this.repository = repository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReceiptResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceiptResponse findById(UUID id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public InventoryReceiptResponse create(InventoryReceiptRequest request) {
        String number = request.getReceiptNumber().trim();
        if (repository.existsByReceiptNumber(number)) {
            throw new ConflictException("Số phiếu đã tồn tại: " + number);
        }
        String status = normalizeStatus(request.getStatus(), PENDING);
        if (CANCELLED.equals(status)) {
            throw new IllegalArgumentException("Không tạo phiếu ở trạng thái CANCELLED");
        }

        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setReceiptNumber(number);
        receipt.setSupplierName(request.getSupplierName().trim());
        receipt.setNotes(request.getNotes());
        receipt.setStatus(status);
        receipt.setInventoryReceiptItems(buildItems(receipt, request.getItems()));
        receipt.setTotalAmount(total(receipt));

        InventoryReceipt saved = repository.save(receipt);
        if (APPROVED.equals(status)) {
            applyStock(saved);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryReceiptResponse update(UUID id, InventoryReceiptRequest request) {
        InventoryReceipt receipt = find(id);
        if (!PENDING.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Phiếu đã " + receipt.getStatus() + " nên không thể sửa");
        }
        String newStatus = normalizeStatus(request.getStatus(), receipt.getStatus());

        String number = request.getReceiptNumber().trim();
        if (!number.equals(receipt.getReceiptNumber())) {
            if (repository.existsByReceiptNumber(number)) {
                throw new ConflictException("Số phiếu đã tồn tại: " + number);
            }
            receipt.setReceiptNumber(number);
        }
        receipt.setSupplierName(request.getSupplierName().trim());
        receipt.setNotes(request.getNotes());

        // Thay toàn bộ dòng chi tiết (giữ nguyên đối tượng danh sách để Hibernate xóa dòng cũ)
        receipt.getInventoryReceiptItems().clear();
        receipt.getInventoryReceiptItems().addAll(buildItems(receipt, request.getItems()));
        receipt.setTotalAmount(total(receipt));
        receipt.setStatus(newStatus);

        InventoryReceipt saved = repository.save(receipt);
        if (APPROVED.equals(newStatus)) {
            applyStock(saved);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        InventoryReceipt receipt = find(id);
        if (APPROVED.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Phiếu đã duyệt (đã cộng kho) nên không thể xóa");
        }
        repository.delete(receipt);
    }

    private InventoryReceipt find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("InventoryReceipt not found with id: " + id));
    }

    private String normalizeStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        String s = status.trim().toUpperCase();
        if (!STATUSES.contains(s)) {
            throw new IllegalArgumentException("Trạng thái phiếu không hợp lệ: " + status + " (PENDING, APPROVED, CANCELLED)");
        }
        return s;
    }

    private List<InventoryReceiptItem> buildItems(InventoryReceipt receipt, List<InventoryReceiptItemRequest> requests) {
        List<InventoryReceiptItem> items = new ArrayList<>();
        for (InventoryReceiptItemRequest r : requests) {
            ProductVariant variant = variantRepository.findById(r.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundEx("Product variant not found with id: " + r.getProductVariantId()));
            InventoryReceiptItem item = new InventoryReceiptItem();
            item.setInventoryReceipt(receipt);
            item.setProduct(variant.getProduct());
            item.setProductVariant(variant);
            item.setQuantity(r.getQuantity());
            item.setUnitCost(r.getUnitCost());
            item.setTotalCost(r.getUnitCost().multiply(BigDecimal.valueOf(r.getQuantity())));
            items.add(item);
        }
        return items;
    }

    private BigDecimal total(InventoryReceipt receipt) {
        return receipt.getInventoryReceiptItems().stream()
                .map(InventoryReceiptItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Duyệt phiếu: cộng số lượng vào tồn kho từng biến thể và ghi sổ biến động kho
    private void applyStock(InventoryReceipt receipt) {
        for (InventoryReceiptItem item : receipt.getInventoryReceiptItems()) {
            ProductVariant variant = stockService.lock(item.getProductVariant().getId());
            stockService.add(variant, item.getQuantity(), receipt.getId(), "RECEIPT", "Nhập kho theo phiếu " + receipt.getReceiptNumber());
        }
    }

    private InventoryReceiptResponse toResponse(InventoryReceipt r) {
        List<InventoryReceiptItemResponse> items = r.getInventoryReceiptItems() == null ? List.of()
                : r.getInventoryReceiptItems().stream().map(i -> InventoryReceiptItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productVariantId(i.getProductVariant().getId())
                        .productName(i.getProduct().getName())
                        .color(i.getProductVariant().getColor())
                        .size(i.getProductVariant().getSize())
                        .quantity(i.getQuantity())
                        .unitCost(i.getUnitCost())
                        .totalCost(i.getTotalCost())
                        .build()).toList();
        return InventoryReceiptResponse.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .supplierName(r.getSupplierName())
                .receiptDate(r.getReceiptDate())
                .totalAmount(r.getTotalAmount())
                .status(r.getStatus())
                .notes(r.getNotes())
                .items(items)
                .build();
    }
}
