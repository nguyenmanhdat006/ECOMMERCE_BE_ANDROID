package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ReviewRequest;
import com.nguyendat.shopee_be.dto.ReviewResponse;
import com.nguyendat.shopee_be.dto.ReviewUpdateRequest;
import com.nguyendat.shopee_be.dto.ReviewerDto;
import com.nguyendat.shopee_be.entities.Order;
import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.entities.OrderStatus;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.Review;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import com.nguyendat.shopee_be.exceptions.ForbiddenException;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.OrderItemRepository;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.repositories.ReviewRepository;
import com.nguyendat.shopee_be.ultil.AccessUtil;
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

    /** Viết đánh giá: chỉ cho dòng đơn thuộc đơn của chính người dùng, đơn đã hoàn thành, mỗi dòng một đánh giá. */
    @Transactional
    public ReviewResponse createReview(User user, ReviewRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundEx("Order item not found"));
        Order order = orderItem.getOrder();

        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá sản phẩm này!");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Chỉ đánh giá được sau khi đơn hàng hoàn thành");
        }
        Product product = orderItem.getProduct();
        if (!product.getId().equals(request.getProductId())) {
            throw new IllegalArgumentException("Sản phẩm không khớp với dòng đơn hàng");
        }
        if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), orderItem.getId())) {
            throw new ConflictException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        reviewRepository.save(review);

        orderItem.setIsReviewed(true);
        orderItemRepository.save(orderItem);

        updateProductRating(product.getId());
        return toDto(review);
    }

    /** Tính lại điểm trung bình của sản phẩm. */
    public void updateProductRating(UUID productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundEx("Product not found"));

        product.setRating((float) avg);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(UUID productId) {
        return reviewRepository.findByProductId(productId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ReviewResponse updateReview(User user, UUID reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundEx("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền sửa đánh giá này!");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);

        updateProductRating(review.getProduct().getId());
        return toDto(review);
    }

    /** Chủ đánh giá hoặc ADMIN được xóa; xóa xong dòng đơn được đánh giá lại. */
    @Transactional
    public void deleteReview(User user, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundEx("Review not found"));
        if (!AccessUtil.isOwnerOrAdmin(user, review.getUser().getId())) {
            throw new ForbiddenException("Bạn không có quyền xóa đánh giá này!");
        }

        UUID productId = review.getProduct().getId();
        OrderItem orderItem = review.getOrderItem();
        reviewRepository.delete(review);

        if (orderItem != null) {
            orderItem.setIsReviewed(false);
            orderItemRepository.save(orderItem);
        }
        updateProductRating(productId);
    }

    private ReviewResponse toDto(Review r) {
        ReviewerDto reviewer = null;
        if (r.getUser() != null) {
            User u = r.getUser();
            reviewer = ReviewerDto.builder()
                    .id(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .avatar(u.getAvatar())
                    .build();
        }
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .user(reviewer)
                .build();
    }
}
