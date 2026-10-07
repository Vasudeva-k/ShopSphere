package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.entity.WishlistItem;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import com.vasudev.shopsphere.repository.WishlistItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct;
    private WishlistItem testItem;

    @BeforeEach
    void setUp() {
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testProduct = new Product("Wireless Headphones", "Noise cancelling", new BigDecimal("199.99"), 30, null);
        testProduct.setId(10L);

        testItem = new WishlistItem(testUser, testProduct);
        testItem.setId(100L);
        testItem.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    // ==========================================
    // 1. addItem Tests
    // ==========================================

    @Test
    void addItem_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(wishlistItemRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> {
            WishlistItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });

        WishlistItem saved = wishlistService.addItem(1L, 10L);

        assertNotNull(saved);
        assertEquals(100L, saved.getId());
        assertEquals(testUser, saved.getUser());
        assertEquals(testProduct, saved.getProduct());

        verify(userRepository).findById(1L);
        verify(productRepository).findById(10L);
        verify(wishlistItemRepository).findByUserAndProduct(testUser, testProduct);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void addItem_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.addItem(999L, 10L));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(wishlistItemRepository);
    }

    @Test
    void addItem_whenProductNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.addItem(1L, 999L));

        verify(wishlistItemRepository, never()).findByUserAndProduct(any(), any());
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenDuplicateItem_throwsIllegalStateException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(wishlistItemRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.of(testItem));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                wishlistService.addItem(1L, 10L)
        );

        assertEquals("Product is already in the wishlist", ex.getMessage());
        verify(wishlistItemRepository, never()).save(any());
    }

    // ==========================================
    // 2. getWishlist Tests
    // ==========================================

    @Test
    void getWishlist_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), pageable, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByUser(testUser, pageable)).thenReturn(page);

        Page<WishlistItem> result = wishlistService.getWishlist(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());

        verify(wishlistItemRepository).findByUser(testUser, pageable);
    }

    @Test
    void getWishlist_whenUserNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.getWishlist(999L, pageable));

        verifyNoInteractions(wishlistItemRepository);
    }

    // ==========================================
    // 3. getItem Tests
    // ==========================================

    @Test
    void getItem_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testItem));

        WishlistItem result = wishlistService.getItem(1L, 100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(testUser, result.getUser());
        assertEquals(testProduct, result.getProduct());
    }

    @Test
    void getItem_whenItemNotFoundOrWrongOwner_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.getItem(1L, 999L));
    }

    @Test
    void getItem_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.getItem(999L, 100L));

        verifyNoInteractions(wishlistItemRepository);
    }

    // ==========================================
    // 4. removeItem Tests
    // ==========================================

    @Test
    void removeItem_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testItem));

        wishlistService.removeItem(1L, 100L);

        verify(wishlistItemRepository).delete(testItem);
    }

    @Test
    void removeItem_whenItemNotFoundOrWrongOwner_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.removeItem(1L, 999L));

        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void removeItem_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.removeItem(999L, 100L));

        verifyNoInteractions(wishlistItemRepository);
    }

    // ==========================================
    // 5. isProductInWishlist Tests
    // ==========================================

    @Test
    void isProductInWishlist_whenPresent_returnsTrue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(wishlistItemRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.of(testItem));

        boolean present = wishlistService.isProductInWishlist(1L, 10L);

        assertTrue(present);
        verify(wishlistItemRepository).findByUserAndProduct(testUser, testProduct);
    }

    @Test
    void isProductInWishlist_whenAbsent_returnsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(wishlistItemRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());

        boolean present = wishlistService.isProductInWishlist(1L, 10L);

        assertFalse(present);
        verify(wishlistItemRepository).findByUserAndProduct(testUser, testProduct);
    }

    @Test
    void isProductInWishlist_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.isProductInWishlist(999L, 10L));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(wishlistItemRepository);
    }

    @Test
    void isProductInWishlist_whenProductNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.isProductInWishlist(1L, 999L));

        verify(wishlistItemRepository, never()).findByUserAndProduct(any(), any());
    }

    // ==========================================
    // 6. clearWishlist Tests
    // ==========================================

    @Test
    void clearWishlist_success() {
        WishlistItem item2 = new WishlistItem(testUser, new Product("Mouse", "Ergonomic", new BigDecimal("49.99"), 100, null));
        item2.setId(101L);
        List<WishlistItem> items = List.of(testItem, item2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByUser(testUser)).thenReturn(items);

        wishlistService.clearWishlist(1L);

        verify(wishlistItemRepository).deleteAll(items);
    }

    @Test
    void clearWishlist_whenEmpty_callsDeleteAllWithEmptyList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistItemRepository.findByUser(testUser)).thenReturn(List.of());

        wishlistService.clearWishlist(1L);

        verify(wishlistItemRepository).deleteAll(List.of());
    }

    @Test
    void clearWishlist_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.clearWishlist(999L));

        verifyNoInteractions(wishlistItemRepository);
    }

    // ==========================================
    // Ownership Isolation Tests
    // ==========================================

    @Test
    void userCannotAccessOrDeleteAnotherUsersWishlistItem() {
        User otherUser = new User("Bob", "Johnson", "bob@example.com", "Password123", "0987654321");
        otherUser.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(wishlistItemRepository.findByIdAndUser(100L, otherUser)).thenReturn(Optional.empty());

        // Cannot read
        assertThrows(ResourceNotFoundException.class, () ->
                wishlistService.getItem(2L, 100L)
        );

        // Cannot delete
        assertThrows(ResourceNotFoundException.class, () ->
                wishlistService.removeItem(2L, 100L)
        );

        verify(wishlistItemRepository, never()).delete(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {WishlistService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("wishlist");
            }

            @Bean
            public WishlistItemRepository wishlistItemRepository() {
                return Mockito.mock(WishlistItemRepository.class);
            }

            @Bean
            public UserRepository userRepository() {
                return Mockito.mock(UserRepository.class);
            }

            @Bean
            public ProductRepository productRepository() {
                return Mockito.mock(ProductRepository.class);
            }
        }

        @Autowired
        private WishlistService wishlistService;

        @Autowired
        private WishlistItemRepository wishlistItemRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user1;
        private User user2;
        private Product product;
        private WishlistItem item1;
        private WishlistItem item2;

        @BeforeEach
        void setUp() {
            Mockito.reset(wishlistItemRepository, userRepository, productRepository);
            Cache cache = cacheManager.getCache("wishlist");
            if (cache != null) {
                cache.clear();
            }

            user1 = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
            user1.setId(1L);

            user2 = new User("Bob", "Jones", "bob@example.com", "Password123", "0987654321");
            user2.setId(2L);

            product = new Product("Headphones", "Noise cancelling", new BigDecimal("199.99"), 30, null);
            product.setId(10L);

            item1 = new WishlistItem(user1, product);
            item1.setId(101L);

            item2 = new WishlistItem(user2, product);
            item2.setId(102L);
        }

        @Test
        void testA_and_testB_firstLookup_and_cacheHit() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistItem> page = new PageImpl<>(List.of(item1), pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, pageable)).thenReturn(page);

            // Test A — First lookup (Cache Miss)
            Page<WishlistItem> first = wishlistService.getWishlist(1L, pageable);
            assertNotNull(first);
            assertEquals(1, first.getTotalElements());
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);

            // Test B — Second lookup (Cache Hit)
            Page<WishlistItem> second = wishlistService.getWishlist(1L, pageable);
            assertNotNull(second);
            assertEquals(1, second.getTotalElements());
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);
        }

        @Test
        void testC_userIsolation() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistItem> page1 = new PageImpl<>(List.of(item1), pageable, 1);
            Page<WishlistItem> page2 = new PageImpl<>(List.of(item2), pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(wishlistItemRepository.findByUser(user1, pageable)).thenReturn(page1);
            when(wishlistItemRepository.findByUser(user2, pageable)).thenReturn(page2);

            Page<WishlistItem> r1 = wishlistService.getWishlist(1L, pageable);
            assertEquals(101L, r1.getContent().get(0).getId());
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);

            Page<WishlistItem> r2 = wishlistService.getWishlist(2L, pageable);
            assertEquals(102L, r2.getContent().get(0).getId());
            verify(wishlistItemRepository, times(1)).findByUser(user2, pageable);
        }

        @Test
        void testD_pageIsolation() {
            Pageable page0 = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Pageable page1 = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<WishlistItem> p0 = new PageImpl<>(List.of(item1), page0, 2);
            Page<WishlistItem> p1 = new PageImpl<>(List.of(item1), page1, 2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, page0)).thenReturn(p0);
            when(wishlistItemRepository.findByUser(user1, page1)).thenReturn(p1);

            wishlistService.getWishlist(1L, page0);
            verify(wishlistItemRepository, times(1)).findByUser(user1, page0);

            wishlistService.getWishlist(1L, page1);
            verify(wishlistItemRepository, times(1)).findByUser(user1, page1);
        }

        @Test
        void testE_sortIsolation() {
            Pageable sortDesc = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Pageable sortAsc = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"));

            Page<WishlistItem> pDesc = new PageImpl<>(List.of(item1), sortDesc, 1);
            Page<WishlistItem> pAsc = new PageImpl<>(List.of(item1), sortAsc, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, sortDesc)).thenReturn(pDesc);
            when(wishlistItemRepository.findByUser(user1, sortAsc)).thenReturn(pAsc);

            wishlistService.getWishlist(1L, sortDesc);
            verify(wishlistItemRepository, times(1)).findByUser(user1, sortDesc);

            wishlistService.getWishlist(1L, sortAsc);
            verify(wishlistItemRepository, times(1)).findByUser(user1, sortAsc);
        }

        @Test
        void testF_addEviction() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistItem> page = new PageImpl<>(List.of(item1), pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, pageable)).thenReturn(page);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(wishlistItemRepository.findByUserAndProduct(user1, product)).thenReturn(Optional.empty());
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(inv -> inv.getArgument(0));

            // Populate cache
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);

            // Add item
            wishlistService.addItem(1L, 10L);

            // Next getWishlist should hit repository again
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(2)).findByUser(user1, pageable);
        }

        @Test
        void testG_removeEviction() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistItem> page = new PageImpl<>(List.of(item1), pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, pageable)).thenReturn(page);
            when(wishlistItemRepository.findByIdAndUser(101L, user1)).thenReturn(Optional.of(item1));

            // Populate cache
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);

            // Remove item
            wishlistService.removeItem(1L, 101L);

            // Next getWishlist should hit repository again
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(2)).findByUser(user1, pageable);
        }

        @Test
        void testH_clearEviction() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistItem> page = new PageImpl<>(List.of(item1), pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(wishlistItemRepository.findByUser(user1, pageable)).thenReturn(page);
            when(wishlistItemRepository.findByUser(user1)).thenReturn(List.of(item1));

            // Populate cache
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(1)).findByUser(user1, pageable);

            // Clear wishlist
            wishlistService.clearWishlist(1L);

            // Next getWishlist should hit repository again
            wishlistService.getWishlist(1L, pageable);
            verify(wishlistItemRepository, times(2)).findByUser(user1, pageable);
        }
    }
}
