package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Thông tin công khai của người đánh giá: không có email, số điện thoại.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String avatar;
}
