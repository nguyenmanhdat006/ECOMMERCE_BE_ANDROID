package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ReviewRequest;
import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.Review;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.repositories.OrderItemRepository;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.repositories.ReviewRepository;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserDetailRepository userDetailRepository;

    @Transactional
    public Review createReview(UUID userId, ReviewRequest request) {

        // 1. Validate orderItem tồn tại
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        // 2. Kiểm tra orderItem có thuộc về user hay không
        if (!orderItem.getOrder().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền đánh giá sản phẩm này!");
        }

    // 3. (Removed) Allow multiple reviews per order item by the same user

        // 4. Lấy thông tin user + product
        User user = userDetailRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 5. Tạo Review
        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

    // 7. Đánh dấu order item đã được review
    orderItem.setIsReviewed(true);
    orderItemRepository.save(orderItem);

        // 6. Cập nhật điểm rating trung bình của product
        updateProductRating(product.getId());

        return review;
    }

    /**
     * Tính toán rating trung bình và cập nhật vào Product
     */
    public void updateProductRating(UUID productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setRating((float) avg);
        productRepository.save(product);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.nguyendat.shopee_be.dto.ReviewResponse> getReviewsByProduct(UUID productId) {
        java.util.List<Review> reviews = reviewRepository.findByProductId(productId);

        java.util.List<com.nguyendat.shopee_be.dto.ReviewResponse> dtos = new java.util.ArrayList<>();
        for (Review r : reviews) {
            dtos.add(toDto(r));
        }

        return dtos;
    }

    private com.nguyendat.shopee_be.dto.ReviewResponse toDto(Review r) {
        com.nguyendat.shopee_be.dto.UserDto userDto = null;
        if (r.getUser() != null) {
            com.nguyendat.shopee_be.entities.User u = r.getUser();
            userDto = com.nguyendat.shopee_be.dto.UserDto.builder()
                    .id(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .avatar(u.getAvatar())
                    .email(u.getEmail())
                    .phoneNumber(u.getPhoneNumber())
                    .enabled(u.isEnabled())
                    .build();
        }

        return com.nguyendat.shopee_be.dto.ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .user(userDto)
                .build();
    }

    @Transactional
    public Review updateReview(UUID userId, UUID reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));


        // Only update rating/comment
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        reviewRepository.save(review);

        // Recalculate product rating
        updateProductRating(review.getProduct().getId());

        return review;
    }

    @Transactional
    public void deleteReview(UUID userId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này!");
        }

        UUID productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Recalculate product rating
        updateProductRating(productId);
    }
}
