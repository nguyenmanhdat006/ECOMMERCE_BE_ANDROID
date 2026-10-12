package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.ReviewRequest;
import com.nguyendat.shopee_be.dto.ReviewResponse;
import com.nguyendat.shopee_be.dto.ReviewUpdateRequest;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Người viết luôn là người đang đăng nhập
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@AuthenticationPrincipal User user, @Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(user, request);
    }

    @GetMapping("/product/{productId}")
    public List<ReviewResponse> getReviewsByProduct(@PathVariable("productId") UUID productId) {
        return reviewService.getReviewsByProduct(productId);
    }

    @PutMapping("/{id}")
    public ReviewResponse updateReview(@PathVariable("id") UUID id, @AuthenticationPrincipal User user,
                                       @Valid @RequestBody ReviewUpdateRequest request) {
        return reviewService.updateReview(user, id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable("id") UUID id, @AuthenticationPrincipal User user) {
        reviewService.deleteReview(user, id);
    }
}
