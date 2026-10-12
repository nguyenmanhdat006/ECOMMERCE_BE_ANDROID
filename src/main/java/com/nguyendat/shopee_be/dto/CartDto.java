package com.nguyendat.shopee_be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

// Khi thêm hoặc sửa, userId lấy từ token và productId suy ra từ biến thể nên không cần gửi.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {
    private UUID id;

    @NotNull(message = "Số lượng là bắt buộc")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    private Date createdAt;
    private Date updatedAt;
    private UUID userId;
    private UUID productId;

    @NotNull(message = "productVariantId là bắt buộc")
    private UUID productVariantId;
}
