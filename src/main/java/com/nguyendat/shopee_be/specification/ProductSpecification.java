package com.nguyendat.shopee_be.specification;

import com.nguyendat.shopee_be.entities.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> hasCategoryId(UUID categorId){
        return  (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"),categorId);
    }

    public static Specification<Product> hasCategoryTypeId(UUID typeId){
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryType").get("id"),typeId);
    }
    
    public static Specification<Product> hasKeyword(String keyword){
        return (root, query, criteriaBuilder) -> {
            if(keyword == null || keyword.trim().isEmpty()){
                return criteriaBuilder.conjunction();
            }
            // Tìm không phân biệt hoa thường và dấu tiếng Việt: từ khóa bỏ dấu ở Java, cột bỏ dấu bằng unaccent() của PostgreSQL (migration V3)
            String likePattern = "%" + foldAccents(keyword) + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(unaccentLower(criteriaBuilder, root.get("name")), likePattern),
                criteriaBuilder.like(unaccentLower(criteriaBuilder, root.get("description")), likePattern),
                criteriaBuilder.like(unaccentLower(criteriaBuilder, root.get("brand")), likePattern)
            );
        };
    }
    
    private static jakarta.persistence.criteria.Expression<String> unaccentLower(
            jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> column) {
        return cb.function("unaccent", String.class, cb.lower(column));
    }

    static String foldAccents(String text) {
        String normalized = java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replace('đ', 'd').replace('Đ', 'D').toLowerCase();
    }

    public static Specification<Product> hasBrand(String brand){
        return (root, query, criteriaBuilder) -> {
            if(brand == null || brand.trim().isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("brand")), brand.toLowerCase());
        };
    }
    
    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice){
        return (root, query, criteriaBuilder) -> {
            if(minPrice == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }
    
    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice){
        return (root, query, criteriaBuilder) -> {
            if(maxPrice == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
    
    public static Specification<Product> hasRatingGreaterThanOrEqual(Float minRating){
        return (root, query, criteriaBuilder) -> {
            if(minRating == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
        };
    }
    
    public static Specification<Product> isNewArrival(Boolean isNewArrival){
        return (root, query, criteriaBuilder) -> {
            if(isNewArrival == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isNewArrival"), isNewArrival);
        };
    }
}