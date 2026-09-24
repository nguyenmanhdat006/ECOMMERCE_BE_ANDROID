package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ProductDto;
import com.nguyendat.shopee_be.entities.*;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.mapper.ProductMapper;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.specification.ProductSpecification;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public Product addProduct(ProductDto productDto) {
        Product product = productMapper.mapToProductEntity(productDto);
        return productRepository.save(product);
    }

    @Override
    public List<ProductDto> getAllProducts(UUID categoryId, UUID typeId) {

        Specification<Product> productSpecification= (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if(null != categoryId){
            productSpecification = productSpecification.and(ProductSpecification.hasCategoryId(categoryId));
        }
        if(null != typeId){
            productSpecification = productSpecification.and(ProductSpecification.hasCategoryTypeId(typeId));
        }

        List<Product> products = productRepository.findAll(productSpecification);
        return products.stream().map(product -> {
            ProductDto dto = productMapper.mapProductToDto(product);
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryTypeId(product.getCategoryType().getId());
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategoryTypeName(product.getCategoryType().getName());
            dto.setVariants(productMapper.mapProductVariantListToDto(product.getProductVariants()));
            dto.setProductResources(productMapper.mapProductResourcesListDto(product.getResources()));
            return dto;
        }).toList();
    }

    @Override
    public ProductDto getProductBySlug(String slug) {
        Product product= productRepository.findBySlug(slug);
        if(null == product){
            throw new ResourceNotFoundEx("Product Not Found!");
        }
        ProductDto productDto = productMapper.mapProductToDto(product);
        productDto.setCategoryId(product.getCategory().getId());
        productDto.setCategoryTypeId(product.getCategoryType().getId());
        productDto.setCategoryName(product.getCategory().getName());
        productDto.setCategoryTypeName(product.getCategoryType().getName());
        productDto.setVariants(productMapper.mapProductVariantListToDto(product.getProductVariants()));
        productDto.setProductResources(productMapper.mapProductResourcesListDto(product.getResources()));
        return productDto;
    }

    @Override
    public ProductDto getProductById(UUID id) {
        Product product= productRepository.findById(id).orElseThrow(()-> new ResourceNotFoundEx("Product Not Found!"));
        ProductDto productDto = productMapper.mapProductToDto(product);
        productDto.setCategoryId(product.getCategory().getId());
        productDto.setCategoryTypeId(product.getCategoryType().getId());
        productDto.setCategoryName(product.getCategory().getName());
        productDto.setCategoryTypeName(product.getCategoryType().getName());
        productDto.setVariants(productMapper.mapProductVariantListToDto(product.getProductVariants()));
        productDto.setProductResources(productMapper.mapProductResourcesListDto(product.getResources()));
        return productDto;
    }

    @Override
    public Product updateProduct(ProductDto productDto, UUID id) {
        Product product = productRepository.findById(id).orElseThrow(()-> new ResourceNotFoundEx("Product Not Found!"));
        productDto.setId(product.getId());
        return productRepository.save(productMapper.mapToProductEntity(productDto));
    }

    @Override
    public Product fetchProductById(UUID id) throws Exception {
        return productRepository.findById(id).orElseThrow(BadRequestException::new);
    }

    @Override
    public Page<ProductDto> searchProducts(
            String keyword,
            UUID categoryId,
            UUID typeId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Float minRating,
            Boolean isNewArrival,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        // Build the specification with all filter criteria
        Specification<Product> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            specification = specification.and(ProductSpecification.hasKeyword(keyword));
        }
        
        if (categoryId != null) {
            specification = specification.and(ProductSpecification.hasCategoryId(categoryId));
        }
        
        if (typeId != null) {
            specification = specification.and(ProductSpecification.hasCategoryTypeId(typeId));
        }
        
        if (brand != null && !brand.trim().isEmpty()) {
            specification = specification.and(ProductSpecification.hasBrand(brand));
        }
        
        if (minPrice != null) {
            specification = specification.and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
        }
        
        if (maxPrice != null) {
            specification = specification.and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
        }
        
        if (minRating != null) {
            specification = specification.and(ProductSpecification.hasRatingGreaterThanOrEqual(minRating));
        }
        
        if (isNewArrival != null) {
            specification = specification.and(ProductSpecification.isNewArrival(isNewArrival));
        }
        
        // Build sorting
        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if ("desc".equalsIgnoreCase(sortDirection)) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, sortBy);
        }
        
        // Build pagination
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Execute query
        Page<Product> productPage = productRepository.findAll(specification, pageable);
        
        // Map to DTO
        return productPage.map(product -> {
            ProductDto dto = productMapper.mapProductToDto(product);
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryTypeId(product.getCategoryType().getId());
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategoryTypeName(product.getCategoryType().getName());
            return dto;
        });
    }

    @Override
    public void deleteProductById(UUID id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundEx("Product Not Found!"));
        productRepository.delete(product);
    }

}