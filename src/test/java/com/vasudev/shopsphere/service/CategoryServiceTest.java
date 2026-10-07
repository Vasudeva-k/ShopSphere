package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void saveCategory_savesAndReturnsCategory() {
        Category category = new Category("Electronics", "Tech gadgets");
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category saved = categoryService.saveCategory(category);

        assertNotNull(saved);
        assertEquals("Electronics", saved.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void getCategoryById_whenExists_returnsOptionalWithCategory() {
        Category category = new Category("Electronics", "Tech gadgets");
        category.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Optional<Category> found = categoryService.getCategoryById(1L);

        assertTrue(found.isPresent());
        assertEquals("Electronics", found.get().getName());
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getCategoryById_whenDoesNotExist_returnsEmptyOptional() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Category> found = categoryService.getCategoryById(99L);

        assertFalse(found.isPresent());
        verify(categoryRepository).findById(99L);
    }

    @Test
    void getCategoryByName_whenExists_returnsOptionalWithCategory() {
        Category category = new Category("Electronics", "Tech gadgets");
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(category));

        Optional<Category> found = categoryService.getCategoryByName("Electronics");

        assertTrue(found.isPresent());
        assertEquals("Electronics", found.get().getName());
        verify(categoryRepository).findByName("Electronics");
    }

    @Test
    void getActiveCategories_returnsActiveOnly() {
        Category cat1 = new Category("Active 1", "Desc 1");
        Category cat2 = new Category("Active 2", "Desc 2");
        when(categoryRepository.findByActiveTrue()).thenReturn(List.of(cat1, cat2));

        List<Category> active = categoryService.getActiveCategories();

        assertEquals(2, active.size());
        verify(categoryRepository).findByActiveTrue();
    }

    @Test
    void getAllCategories_returnsAll() {
        Category cat1 = new Category("Cat 1", "Desc 1");
        Category cat2 = new Category("Cat 2", "Desc 2");
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        List<Category> all = categoryService.getAllCategories();

        assertEquals(2, all.size());
        verify(categoryRepository).findAll();
    }

    @Test
    void updateCategory_updatesAndReturnsCategory() {
        Category category = new Category("Updated", "Updated Desc");
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category updated = categoryService.updateCategory(5L, category);

        assertNotNull(updated);
        assertEquals(5L, category.getId());
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_deletesById() {
        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Nested
    @SpringJUnitConfig(classes = {CategoryService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("categories");
            }

            @Bean
            public CategoryRepository categoryRepository() {
                return Mockito.mock(CategoryRepository.class);
            }
        }

        @Autowired
        private CategoryService categoryService;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private CacheManager cacheManager;

        @BeforeEach
        void clearCache() {
            Mockito.reset(categoryRepository);
            Cache cache = cacheManager.getCache("categories");
            if (cache != null) {
                cache.clear();
            }
        }

        @Test
        void testA_successfulGet_and_testB_cachingBehavior() {
            Long categoryId = 1L;
            Category category = new Category("Electronics", "Gadgets");
            category.setId(categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            // First lookup - Cache Miss
            Optional<Category> firstLookup = categoryService.getCategoryById(categoryId);
            assertTrue(firstLookup.isPresent());
            assertEquals("Electronics", firstLookup.get().getName());
            verify(categoryRepository, times(1)).findById(categoryId);

            // Second lookup - Cache Hit
            Optional<Category> secondLookup = categoryService.getCategoryById(categoryId);
            assertTrue(secondLookup.isPresent());
            assertEquals("Electronics", secondLookup.get().getName());
            verify(categoryRepository, times(1)).findById(categoryId);
        }

        @Test
        void testB_missingCategory_returnsEmpty() {
            Long missingId = 999L;
            when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

            Optional<Category> result = categoryService.getCategoryById(missingId);
            assertFalse(result.isPresent());
            verify(categoryRepository, times(1)).findById(missingId);
        }

        @Test
        void testC_updateEviction_evictsAffectedIdOnly() {
            Long categoryId = 2L;
            Category category = new Category("Books", "Printed and audio");
            category.setId(categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            // Populate cache
            categoryService.getCategoryById(categoryId);
            verify(categoryRepository, times(1)).findById(categoryId);

            // Update category
            Category updateData = new Category("Books & Media", "Printed, audio, digital");
            categoryService.updateCategory(categoryId, updateData);

            // Subsequent lookup hits repository again due to cache eviction
            categoryService.getCategoryById(categoryId);
            verify(categoryRepository, times(2)).findById(categoryId);
        }

        @Test
        void testD_deleteEviction_evictsCategoryCacheEntry() {
            Long categoryId = 3L;
            Category category = new Category("Home", "Home decor");
            category.setId(categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            // Populate cache
            categoryService.getCategoryById(categoryId);
            verify(categoryRepository, times(1)).findById(categoryId);

            Cache cache = cacheManager.getCache("categories");
            assertNotNull(cache);
            assertNotNull(cache.get(categoryId));

            // Delete category
            categoryService.deleteCategory(categoryId);

            // Verify cache entry is evicted
            assertNull(cache.get(categoryId));
        }

        @Test
        void testE_cacheIsolation_independentCacheKeys() {
            Long id1 = 10L;
            Long id2 = 20L;

            Category cat1 = new Category("Cat1", "Desc1");
            cat1.setId(id1);

            Category cat2 = new Category("Cat2", "Desc2");
            cat2.setId(id2);

            when(categoryRepository.findById(id1)).thenReturn(Optional.of(cat1));
            when(categoryRepository.findById(id2)).thenReturn(Optional.of(cat2));

            // Populate both caches
            categoryService.getCategoryById(id1);
            categoryService.getCategoryById(id2);

            verify(categoryRepository, times(1)).findById(id1);
            verify(categoryRepository, times(1)).findById(id2);

            // Evict id1 via update
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
            categoryService.updateCategory(id1, cat1);

            // id1 should miss cache, id2 should still hit cache
            categoryService.getCategoryById(id1);
            categoryService.getCategoryById(id2);

            verify(categoryRepository, times(2)).findById(id1);
            verify(categoryRepository, times(1)).findById(id2);
        }
    }
}
