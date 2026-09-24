package com.nguyendat.shopee_be.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ReviewRequest {
    private UUID userId;
    private UUID productId;
    private UUID orderItemId;
    private int rating;
    private String comment;
}
