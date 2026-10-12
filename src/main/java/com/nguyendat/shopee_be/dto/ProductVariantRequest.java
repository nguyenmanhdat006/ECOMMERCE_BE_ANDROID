package com.nguyendat.shopee_be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    // Bắt buộc khi tạo; bỏ qua khi sửa (không đổi sản phẩm của biến thể)
    private UUID productId;

    @NotBlank(message = "Màu sắc là bắt buộc")
    private String color;

    @NotBlank(message = "Kích cỡ là bắt buộc")
    private String size;

    @NotNull(message = "Tồn kho là bắt buộc")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer stockQuantity;
}
