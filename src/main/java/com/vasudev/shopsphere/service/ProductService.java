package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.dto.ProductCreateRequest;
import com.vasudev.shopsphere.dto.ProductResponse;
import com.vasudev.shopsphere.dto.ProductUpdateRequest;
import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.CategoryRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "stockQuantity", "createdAt", "updatedAt"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<ProductResponse> getProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String search,
            Boolean enabled,
            Long categoryId
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(effectiveSortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }

        String effectiveDirection = (direction != null && !direction.isBlank()) ? direction : "desc";
        if (!"asc".equalsIgnoreCase(effectiveDirection) && !"desc".equalsIgnoreCase(effectiveDirection)) {
            throw new IllegalArgumentException("Invalid sort direction: " + direction);
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(effectiveDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, effectiveSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Product> productPage;
        if (normalizedSearch != null) {
            if (enabled != null && categoryId != null) {
                productPage = productRepository.searchByEnabledAndCategoryId(normalizedSearch, enabled, categoryId, pageable);
            } else if (enabled != null) {
                productPage = productRepository.searchByEnabled(normalizedSearch, enabled, pageable);
            } else if (categoryId != null) {
                productPage = productRepository.searchByCategoryId(normalizedSearch, categoryId, pageable);
            } else {
                productPage = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        normalizedSearch, normalizedSearch, pageable
                );
            }
        } else {
            if (enabled != null && categoryId != null) {
                productPage = productRepository.findByEnabledAndCategoryId(enabled, categoryId, pageable);
            } else if (enabled != null) {
                productPage = productRepository.findByEnabled(enabled, pageable);
            } else if (categoryId != null) {
                productPage = productRepository.findByCategoryId(categoryId, pageable);
            } else {
                productPage = productRepository.findAll(pageable);
            }
        }

        return productPage.map(this::toResponse);
    }

    public Page<ProductResponse> getProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean enabled,
            Long categoryId
    ) {
        return getProducts(page, size, sortBy, direction, null, enabled, categoryId);
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getEnabled() != null) {
            product.setEnabled(request.getEnabled());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getEnabled(),
                categoryId,
                product.getImageUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
