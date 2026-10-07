package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.dto.ProductCreateRequest;
import com.vasudev.shopsphere.dto.ProductResponse;
import com.vasudev.shopsphere.dto.ProductUpdateRequest;
import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.CategoryRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository);
    }

    @Test
    void createProduct_withoutCategory_savesAndReturnsResponse() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Smartphone",
                "Flagship phone",
                new BigDecimal("799.99"),
                50,
                true,
                null
        );

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Smartphone", response.getName());
        assertEquals("Flagship phone", response.getDescription());
        assertEquals(new BigDecimal("799.99"), response.getPrice());
        assertEquals(50, response.getStockQuantity());
        assertTrue(response.getEnabled());
        assertNull(response.getCategoryId());

        verify(categoryRepository, never()).findById(any());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_withValidCategory_savesAndReturnsResponseWithCategoryId() {
        Category category = new Category("Electronics", "Tech gadgets");
        category.setId(10L);

        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "Powerful laptop",
                new BigDecimal("1299.99"),
                20,
                true,
                10L
        );

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Laptop", response.getName());
        assertEquals(10L, response.getCategoryId());

        verify(categoryRepository).findById(10L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_withNonexistentCategory_throwsResourceNotFoundException() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "Powerful laptop",
                new BigDecimal("1299.99"),
                20,
                true,
                999L
        );

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(request));

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_whenProductExists_returnsResponse() {
        Product product = new Product("Headphones", "Noise cancelling", new BigDecimal("199.99"), 100, null);
        product.setId(5L);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(5L);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Headphones", response.getName());
    }

    @Test
    void getProductById_whenProductDoesNotExist_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getAllProducts_returnsListOfResponses() {
        Product p1 = new Product("Item 1", "Desc 1", new BigDecimal("10.00"), 5, null);
        p1.setId(1L);
        Product p2 = new Product("Item 2", "Desc 2", new BigDecimal("20.00"), 10, null);
        p2.setId(2L);

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Item 2", result.get(1).getName());
    }

    @Test
    void getProducts_withDefaults_callsFindAllWithDefaultPageable() {
        Product p1 = new Product("Item 1", "Desc 1", new BigDecimal("10.00"), 5, null);
        p1.setId(1L);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(p1)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProducts_withCustomPageAndSize_setsPageableCorrectly() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        productService.getProducts(2, 10, "price", "asc", null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("price").getDirection());
    }

    @Test
    void getProducts_withAscendingSort_setsAscDirection() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        productService.getProducts(0, 20, "name", "asc", null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void getProducts_withDescendingSort_setsDescDirection() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        productService.getProducts(0, 20, "stockQuantity", "desc", null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("stockQuantity").getDirection());
    }

    @Test
    void getProducts_withInvalidSortField_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                productService.getProducts(0, 20, "unsupportedField", "desc", null, null, null));
    }

    @Test
    void getProducts_withInvalidDirection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                productService.getProducts(0, 20, "createdAt", "diagonal", null, null, null));
    }

    @Test
    void getProducts_withInvalidSizeBelow1_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                productService.getProducts(0, 0, "createdAt", "desc", null, null, null));
    }

    @Test
    void getProducts_withInvalidSizeAbove100_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                productService.getProducts(0, 101, "createdAt", "desc", null, null, null));
    }

    @Test
    void getProducts_withEnabledFilterOnly_callsFindByEnabled() {
        Product p = new Product("Enabled Item", "Desc", new BigDecimal("15.00"), 10, null);
        p.setId(1L);

        when(productRepository.findByEnabled(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", null, true, null);

        verify(productRepository).findByEnabled(eq(true), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
        assertEquals(1, result.getContent().size());
        assertEquals("Enabled Item", result.getContent().get(0).getName());
    }

    @Test
    void getProducts_withCategoryFilterOnly_callsFindByCategoryId() {
        Product p = new Product("Category Item", "Desc", new BigDecimal("25.00"), 5, null);
        p.setId(2L);

        when(productRepository.findByCategoryId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", null, null, 10L);

        verify(productRepository).findByCategoryId(eq(10L), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
        assertEquals(1, result.getContent().size());
        assertEquals("Category Item", result.getContent().get(0).getName());
    }

    @Test
    void getProducts_withEnabledAndCategoryFilter_callsFindByEnabledAndCategoryId() {
        Product p = new Product("Both Filter Item", "Desc", new BigDecimal("35.00"), 8, null);
        p.setId(3L);

        when(productRepository.findByEnabledAndCategoryId(eq(false), eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", null, false, 10L);

        verify(productRepository).findByEnabledAndCategoryId(eq(false), eq(10L), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
        assertEquals(1, result.getContent().size());
        assertEquals("Both Filter Item", result.getContent().get(0).getName());
    }

    @Test
    void getProducts_withSearch_callsFindByNameOrDescription() {
        Product p = new Product("iPhone 15", "Apple smartphone", new BigDecimal("999.00"), 10, null);
        p.setId(1L);

        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("phone"), eq("phone"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", "phone", null, null);

        verify(productRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("phone"), eq("phone"), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
        assertEquals(1, result.getContent().size());
        assertEquals("iPhone 15", result.getContent().get(0).getName());
    }

    @Test
    void getProducts_withSearchAndEnabled_callsSearchByEnabled() {
        Product p = new Product("iPhone 15", "Apple smartphone", new BigDecimal("999.00"), 10, null);
        p.setId(1L);

        when(productRepository.searchByEnabled(eq("phone"), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", "phone", true, null);

        verify(productRepository).searchByEnabled(eq("phone"), eq(true), any(Pageable.class));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProducts_withSearchAndCategoryId_callsSearchByCategoryId() {
        Product p = new Product("iPhone 15", "Apple smartphone", new BigDecimal("999.00"), 10, null);
        p.setId(1L);

        when(productRepository.searchByCategoryId(eq("phone"), eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", "phone", null, 2L);

        verify(productRepository).searchByCategoryId(eq("phone"), eq(2L), any(Pageable.class));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProducts_withSearchAndEnabledAndCategoryId_callsSearchByEnabledAndCategoryId() {
        Product p = new Product("iPhone 15", "Apple smartphone", new BigDecimal("999.00"), 10, null);
        p.setId(1L);

        when(productRepository.searchByEnabledAndCategoryId(eq("phone"), eq(true), eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<ProductResponse> result = productService.getProducts(0, 20, "createdAt", "desc", "phone", true, 2L);

        verify(productRepository).searchByEnabledAndCategoryId(eq("phone"), eq(true), eq(2L), any(Pageable.class));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProducts_withBlankSearch_treatedAsNoSearch() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        productService.getProducts(0, 20, "createdAt", "desc", "   ", null, null);

        verify(productRepository).findAll(any(Pageable.class));
        verify(productRepository, never()).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void getProducts_withWhitespaceAroundSearch_trimsSearchTerm() {
        Product p = new Product("MacBook Pro", "M3 Max laptop", new BigDecimal("2499.00"), 5, null);
        p.setId(1L);

        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("laptop"), eq("laptop"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        productService.getProducts(0, 20, "createdAt", "desc", "   laptop   ", null, null);

        verify(productRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("laptop"), eq("laptop"), any(Pageable.class));
    }

    @Test
    void getProducts_withSearchPreservesPaginationAndSorting() {
        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("phone"), eq("phone"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.getProducts(2, 10, "price", "asc", "phone", null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("phone"), eq("phone"), captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("price").getDirection());
    }

    @Test
    void getProducts_mapsPageOfProductToPageOfProductResponse() {
        Category cat = new Category("Tech", "Tech desc");
        cat.setId(99L);
        Product product = new Product("Mapped Item", "Mapped Desc", new BigDecimal("100.00"), 12, cat);
        product.setId(7L);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        Page<ProductResponse> page = productService.getProducts(0, 20, "createdAt", "desc", null, null, null);

        assertEquals(1, page.getTotalElements());
        ProductResponse item = page.getContent().get(0);
        assertEquals(7L, item.getId());
        assertEquals("Mapped Item", item.getName());
        assertEquals("Mapped Desc", item.getDescription());
        assertEquals(new BigDecimal("100.00"), item.getPrice());
        assertEquals(12, item.getStockQuantity());
        assertEquals(99L, item.getCategoryId());
    }

    @Test
    void updateProduct_partiallyUpdatesFields() {
        Product existing = new Product("Old Name", "Old Desc", new BigDecimal("50.00"), 10, null);
        existing.setId(3L);

        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("New Name");
        updateRequest.setPrice(new BigDecimal("75.00"));

        ProductResponse response = productService.updateProduct(3L, updateRequest);

        assertEquals("New Name", response.getName());
        assertEquals("Old Desc", response.getDescription());
        assertEquals(new BigDecimal("75.00"), response.getPrice());
        assertEquals(10, response.getStockQuantity());
    }

    @Test
    void updateProduct_updatesCategoryWhenProvided() {
        Product existing = new Product("Item", "Desc", new BigDecimal("50.00"), 10, null);
        existing.setId(3L);

        Category newCategory = new Category("Books", "Reading materials");
        newCategory.setId(25L);

        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(25L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setCategoryId(25L);

        ProductResponse response = productService.updateProduct(3L, updateRequest);

        assertEquals(25L, response.getCategoryId());
    }

    @Test
    void updateProduct_withNonexistentCategory_throwsResourceNotFoundException() {
        Product existing = new Product("Item", "Desc", new BigDecimal("50.00"), 10, null);
        existing.setId(3L);

        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setCategoryId(999L);

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(3L, updateRequest));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_whenProductNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("Any");

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(404L, updateRequest));
    }

    @Test
    void deleteProduct_whenProductExists_deletesSuccessfully() {
        when(productRepository.existsById(7L)).thenReturn(true);

        productService.deleteProduct(7L);

        verify(productRepository).deleteById(7L);
    }

    @Test
    void deleteProduct_whenProductDoesNotExist_throwsResourceNotFoundException() {
        when(productRepository.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(7L));

        verify(productRepository, never()).deleteById(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {ProductService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("products");
            }

            @Bean
            public ProductRepository productRepository() {
                return Mockito.mock(ProductRepository.class);
            }

            @Bean
            public CategoryRepository categoryRepository() {
                return Mockito.mock(CategoryRepository.class);
            }
        }

        @Autowired
        private ProductService productService;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CacheManager cacheManager;

        @BeforeEach
        void clearCache() {
            Mockito.reset(productRepository);
            Cache cache = cacheManager.getCache("products");
            if (cache != null) {
                cache.clear();
            }
        }

        @Test
        void cachingLifecycle_firstLookup_missesCache_secondLookup_hitsCache() {
            Long productId = 100L;
            Product product = new Product("CachedItem", "Description", new BigDecimal("99.99"), 10, true, null);
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Scenario A — First lookup (Cache Miss)
            ProductResponse firstResponse = productService.getProductById(productId);
            assertNotNull(firstResponse);
            assertEquals("CachedItem", firstResponse.getName());
            verify(productRepository, times(1)).findById(productId);

            // Scenario B — Second lookup (Cache Hit)
            ProductResponse secondResponse = productService.getProductById(productId);
            assertNotNull(secondResponse);
            assertEquals("CachedItem", secondResponse.getName());
            verify(productRepository, times(1)).findById(productId);
        }

        @Test
        void cachingLifecycle_updateInvalidation_evictsCache() {
            Long productId = 101L;
            Product product = new Product("OldName", "Description", new BigDecimal("50.00"), 5, true, null);
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            // Scenario A - Populate cache
            productService.getProductById(productId);
            verify(productRepository, times(1)).findById(productId);

            // Scenario C - Update invalidation
            ProductUpdateRequest updateRequest = new ProductUpdateRequest();
            updateRequest.setName("NewName");
            productService.updateProduct(productId, updateRequest);

            // Subsequent lookup should hit repository again because update evicted the cache
            productService.getProductById(productId);
            verify(productRepository, times(3)).findById(productId);
        }

        @Test
        void cachingLifecycle_deleteInvalidation_evictsCache() {
            Long productId = 102L;
            Product product = new Product("ToDelete", "Description", new BigDecimal("20.00"), 2, true, null);
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productRepository.existsById(productId)).thenReturn(true);

            // Populate cache
            productService.getProductById(productId);
            verify(productRepository, times(1)).findById(productId);

            Cache cache = cacheManager.getCache("products");
            assertNotNull(cache);
            assertNotNull(cache.get(productId));

            // Scenario D - Delete invalidation
            productService.deleteProduct(productId);

            assertNull(cache.get(productId));
        }
    }
}
