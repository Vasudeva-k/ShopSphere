package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByEnabled(Boolean enabled, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByEnabledAndCategoryId(Boolean enabled, Long categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name,
            String description,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND p.enabled = :enabled")
    Page<Product> searchByEnabled(
            @Param("search") String search,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND p.category.id = :categoryId")
    Page<Product> searchByCategoryId(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND p.enabled = :enabled AND p.category.id = :categoryId")
    Page<Product> searchByEnabledAndCategoryId(
            @Param("search") String search,
            @Param("enabled") Boolean enabled,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}


