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
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), likePattern)
            );
        };
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