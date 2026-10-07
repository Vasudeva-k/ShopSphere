package com.vasudev.shopsphere.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.repository.CategoryRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoCatalogSeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private DemoCatalogSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DemoCatalogSeeder(categoryRepository, productRepository);
    }

    @Test
    @DisplayName("Should create all 9 categories and 45 products when database is empty")
    void seedCatalog_whenEmptyDatabase_createsAllCategoriesAndProducts() {
        AtomicLong categoryIdSequence = new AtomicLong(1L);
        AtomicLong productIdSequence = new AtomicLong(1L);

        when(categoryRepository.findByName(any(String.class))).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryIdSequence.getAndIncrement());
            return cat;
        });

        when(productRepository.findAll()).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product prod = invocation.getArgument(0);
            prod.setId(productIdSequence.getAndIncrement());
            return prod;
        });

        DemoCatalogSeeder.CatalogSeedResult result = seeder.seedCatalog();

        assertEquals(9, result.getCategoriesCreated());
        assertEquals(45, result.getProductsCreated());

        verify(categoryRepository, times(9)).save(any(Category.class));
        verify(productRepository, times(45)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should be idempotent and create 0 categories and 0 products when already seeded")
    void seedCatalog_whenAlreadySeeded_createsNothing() {
        List<Category> existingCategories = new ArrayList<>();
        List<Product> existingProducts = new ArrayList<>();
        long catId = 1L;
        long prodId = 1L;

        for (DemoCatalogSeeder.CategorySeedData catData : seeder.getCategorySeedData()) {
            Category c = new Category(catData.name, catData.description);
            c.setId(catId++);
            existingCategories.add(c);
            when(categoryRepository.findByName(catData.name)).thenReturn(Optional.of(c));

            for (DemoCatalogSeeder.ProductSeedData prodData : catData.products) {
                Product p = new Product(prodData.name, prodData.description, prodData.price, prodData.stockQuantity, true, c, prodData.imageUrl);
                p.setId(prodId++);
                existingProducts.add(p);
            }
        }

        when(productRepository.findAll()).thenReturn(existingProducts);

        DemoCatalogSeeder.CatalogSeedResult result = seeder.seedCatalog();

        assertEquals(0, result.getCategoriesCreated());
        assertEquals(0, result.getProductsCreated());

        verify(categoryRepository, never()).save(any(Category.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("All 45 demo products must have valid HTTPS image URLs")
    void demoProducts_allHaveValidHttpsImageUrls() {
        List<DemoCatalogSeeder.CategorySeedData> categories = seeder.getCategorySeedData();
        assertEquals(9, categories.size());

        int totalProducts = 0;
        for (DemoCatalogSeeder.CategorySeedData cat : categories) {
            for (DemoCatalogSeeder.ProductSeedData prod : cat.products) {
                totalProducts++;
                assertNotNull(prod.imageUrl, "Image URL should not be null for " + prod.name);
                assertTrue(prod.imageUrl.startsWith("https://"), "Image URL must start with https:// for " + prod.name);
                assertFalse(prod.imageUrl.isBlank(), "Image URL must not be blank for " + prod.name);
            }
        }
        assertEquals(45, totalProducts);
    }

    @Test
    @DisplayName("Should backfill imageUrl when existing product has null imageUrl")
    void seedCatalog_whenExistingProductsLackImageUrl_backfillsImageUrl() {
        when(categoryRepository.findByName(any(String.class))).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category cat = inv.getArgument(0);
            cat.setId(1L);
            return cat;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product prod = inv.getArgument(0);
            if (prod.getId() == null) {
                prod.setId(99L);
            }
            return prod;
        });

        Category c = new Category("Electronics & Gadgets", "Tech");
        c.setId(1L);
        when(categoryRepository.findByName("Electronics & Gadgets")).thenReturn(Optional.of(c));

        Product p = new Product("ShopSphere Pro Wireless Noise-Cancelling Headphones", "Desc", new BigDecimal("149.99"), 45, c);
        p.setId(10L);
        p.setImageUrl(null);

        when(productRepository.findAll()).thenReturn(List.of(p));

        seeder.seedCatalog();

        verify(productRepository).save(p);
        assertNotNull(p.getImageUrl());
        assertTrue(p.getImageUrl().startsWith("https://"));
    }

    @Test
    @DisplayName("run method triggers seeding without throwing exceptions")
    void run_executesSeeding() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(categoryRepository.findByName(any(String.class))).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        seeder.run();

        verify(categoryRepository, times(9)).save(any(Category.class));
        verify(productRepository, times(45)).save(any(Product.class));
    }
}
