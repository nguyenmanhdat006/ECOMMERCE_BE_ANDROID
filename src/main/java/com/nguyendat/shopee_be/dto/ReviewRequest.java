package com.nguyendat.shopee_be.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

// Người viết lấy từ token nên không còn trường userId.
@Data
public class ReviewRequest {

    @NotNull(message = "productId là bắt buộc")
    private UUID productId;

    @NotNull(message = "orderItemId là bắt buộc")
    private UUID orderItemId;

    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private int rating;

    private String comment;
}
