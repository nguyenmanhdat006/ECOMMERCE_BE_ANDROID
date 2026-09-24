package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.ReviewRequest;
import com.nguyendat.shopee_be.dto.ReviewResponse;
import com.nguyendat.shopee_be.entities.Review;
import com.nguyendat.shopee_be.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Review createReview(@RequestBody ReviewRequest request) {
        return reviewService.createReview(request.getUserId(), request);
    }


    @GetMapping("/product/{productId}")
    public java.util.List<ReviewResponse> getReviewsByProduct(@PathVariable("productId") UUID productId) {
        return reviewService.getReviewsByProduct(productId);
    }

    @PutMapping("/{id}")
    public Review updateReview(
            @PathVariable("id") UUID id,
            @RequestBody ReviewRequest request
    ) {
        // frontend provides userId in request
        return reviewService.updateReview(request.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(
            @PathVariable("id") UUID id,
            @RequestParam("userId") UUID userId
    ) {
        // frontend provides userId as request parameter
        reviewService.deleteReview(userId, id);
    }
}
