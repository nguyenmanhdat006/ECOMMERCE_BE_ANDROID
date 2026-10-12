package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.ProductDto;
import com.nguyendat.shopee_be.entities.*;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.mapper.ProductMapper;
import com.nguyendat.shopee_be.repositories.CategoryRepository;
import com.nguyendat.shopee_be.repositories.CategoryTypeRepository;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryTypeRepository categoryTypeRepository;

    @Override
    @Transactional
    public ProductDto addProduct(ProductDto productDto) {
        if (isBlank(productDto.getName()) || isBlank(productDto.getBrand()) || isBlank(productDto.getSlug())
                || productDto.getPrice() == null || productDto.getCategoryId() == null || productDto.getCategoryTypeId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin bắt buộc: name, price, brand, slug, categoryId, categoryTypeId");
        }
        if (productDto.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được âm");
        }
        if (productRepository.findBySlug(productDto.getSlug()) != null) {
            throw new ConflictException("Slug đã tồn tại: " + productDto.getSlug());
        }
        productDto.setId(null);
        Product saved = productRepository.save(productMapper.mapToProductEntity(productDto));
        return toDetailDto(saved);
    }

    // Dạng chi tiết: thông tin sản phẩm kèm danh mục, biến thể, ảnh
    private ProductDto toDetailDto(Product product) {
        ProductDto dto = productMapper.mapProductToDto(product);
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryTypeId(product.getCategoryType().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setCategoryTypeName(product.getCategoryType().getName());
        dto.setVariants(productMapper.mapProductVariantListToDto(product.getProductVariants()));
        dto.setProductResources(productMapper.mapProductResourcesListDto(product.getResources()));
        return dto;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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

    // Cập nhật một phần: chỉ ghi các trường có gửi lên. Biến thể và ảnh có API riêng; rating do hệ thống tính.
    @Override
    @Transactional
    public ProductDto updateProduct(ProductDto dto, UUID id) {
        Product product = productRepository.findById(id).orElseThrow(()-> new ResourceNotFoundEx("Product Not Found!"));

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getNewArrival() != null) product.setNewArrival(dto.getNewArrival());
        if (dto.getPrice() != null) {
            if (dto.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Giá sản phẩm không được âm");
            }
            product.setPrice(dto.getPrice());
        }
        if (dto.getSlug() != null && !dto.getSlug().equals(product.getSlug())) {
            if (productRepository.findBySlug(dto.getSlug()) != null) {
                throw new ConflictException("Slug đã tồn tại: " + dto.getSlug());
            }
            product.setSlug(dto.getSlug());
        }
        if (dto.getCategoryId() != null) {
            product.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundEx("Category not found with id: " + dto.getCategoryId())));
        }
        if (dto.getCategoryTypeId() != null) {
            product.setCategoryType(categoryTypeRepository.findById(dto.getCategoryTypeId())
                    .orElseThrow(() -> new ResourceNotFoundEx("CategoryType not found with id: " + dto.getCategoryTypeId())));
        }
        return toDetailDto(productRepository.save(product));
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
    @Transactional
    public void deleteProductById(UUID id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundEx("Product Not Found!"));
        // Quan hệ JPA có cascade nên xóa sản phẩm đã bán sẽ xóa luôn lịch sử đơn hàng, phiếu nhập; chặn từ đầu
        boolean used = !product.getOrderItems().isEmpty() || !product.getInventoryReceiptItems().isEmpty() || !product.getStockMovements().isEmpty();
        if (used) {
            throw new ConflictException("Sản phẩm đã phát sinh đơn hàng, phiếu nhập hoặc biến động kho nên không thể xóa");
        }
        productRepository.delete(product);
    }

}