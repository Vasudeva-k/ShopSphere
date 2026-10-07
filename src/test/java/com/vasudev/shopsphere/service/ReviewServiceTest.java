package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.Review;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.ReviewRepository;
import com.vasudev.shopsphere.repository.UserRepository;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Product testProduct;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testProduct = new Product("Smartphone", "Latest model", new BigDecimal("799.99"), 50, null);
        testProduct.setId(10L);

        testReview = new Review(testUser, testProduct, 5, "Excellent phone!");
        testReview.setId(100L);
        testReview.setCreatedAt(LocalDateTime.now().minusDays(1));
        testReview.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    // ==========================================
    // 1. createReview Tests
    // ==========================================

    @Test
    void createReview_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        Review created = reviewService.createReview(1L, 10L, 5, "Great product!");

        assertNotNull(created);
        assertEquals(100L, created.getId());
        assertEquals(testUser, created.getUser());
        assertEquals(testProduct, created.getProduct());
        assertEquals(5, created.getRating());
        assertEquals("Great product!", created.getComment());

        verify(reviewRepository).findByUserAndProduct(testUser, testProduct);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.createReview(999L, 10L, 5, "Great product!")
        );

        verify(productRepository, never()).findById(any());
        verify(reviewRepository, never()).findByUserAndProduct(any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenProductNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.createReview(1L, 999L, 5, "Great product!")
        );

        verify(reviewRepository, never()).findByUserAndProduct(any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenDuplicateReview_throwsIllegalStateException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.of(testReview));

        assertThrows(IllegalStateException.class, () ->
                reviewService.createReview(1L, 10L, 5, "Another review")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenRatingNull_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(1L, 10L, null, "Comment without rating")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenRatingZero_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(1L, 10L, 0, "Rating 0")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenRatingAboveFive_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(1L, 10L, 6, "Rating 6")
        );

        verify(reviewRepository, never()).save(any());
    }

    // ==========================================
    // 2. getReviewById Tests
    // ==========================================

    @Test
    void getReviewById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));

        Review result = reviewService.getReviewById(1L, 100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(testUser, result.getUser());
        assertEquals(5, result.getRating());
    }

    @Test
    void getReviewById_whenReviewNotFoundOrWrongOwner_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.getReviewById(1L, 999L)
        );
    }

    @Test
    void getReviewById_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.getReviewById(999L, 100L)
        );

        verifyNoInteractions(reviewRepository);
    }

    // ==========================================
    // 3. getReviewsByProduct Tests
    // ==========================================

    @Test
    void getReviewsByProduct_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(testReview), pageable, 1);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByProduct(testProduct, pageable)).thenReturn(reviewPage);

        Page<Review> result = reviewService.getReviewsByProduct(10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }

    @Test
    void getReviewsByProduct_whenProductNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.getReviewsByProduct(999L, pageable)
        );

        verifyNoInteractions(reviewRepository);
    }

    // ==========================================
    // 4. getReviewsByUser Tests
    // ==========================================

    @Test
    void getReviewsByUser_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(testReview), pageable, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByUser(testUser, pageable)).thenReturn(reviewPage);

        Page<Review> result = reviewService.getReviewsByUser(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }

    @Test
    void getReviewsByUser_whenUserNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.getReviewsByUser(999L, pageable)
        );

        verifyNoInteractions(reviewRepository);
    }

    // ==========================================
    // 5. updateReview Tests
    // ==========================================

    @Test
    void updateReview_success() {
        LocalDateTime originalCreatedAt = testReview.getCreatedAt();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review updated = reviewService.updateReview(1L, 100L, 4, "Updated comment: Still good!");

        assertNotNull(updated);
        assertEquals(4, updated.getRating());
        assertEquals("Updated comment: Still good!", updated.getComment());
        assertEquals(testProduct, updated.getProduct());
        assertEquals(testUser, updated.getUser());
        assertEquals(originalCreatedAt, updated.getCreatedAt());

        verify(reviewRepository).save(testReview);
    }

    @Test
    void updateReview_whenReviewNotFoundOrWrongOwner_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.updateReview(1L, 999L, 4, "Updated comment")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.updateReview(999L, 100L, 4, "Updated comment")
        );

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void updateReview_whenRatingNull_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.updateReview(1L, 100L, null, "Updated comment")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenRatingZero_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.updateReview(1L, 100L, 0, "Updated comment")
        );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenRatingAboveFive_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.updateReview(1L, 100L, 6, "Updated comment")
        );

        verify(reviewRepository, never()).save(any());
    }

    // ==========================================
    // 6. deleteReview Tests
    // ==========================================

    @Test
    void deleteReview_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testReview));

        reviewService.deleteReview(1L, 100L);

        verify(reviewRepository).delete(testReview);
    }

    @Test
    void deleteReview_whenReviewNotFoundOrWrongOwner_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.deleteReview(1L, 999L)
        );

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.deleteReview(999L, 100L)
        );

        verifyNoInteractions(reviewRepository);
    }

    // ==========================================
    // Additional Ownership Isolation Test
    // ==========================================

    @Test
    void userCannotAccessOrMutateAnotherUsersReview() {
        User otherUser = new User("Bob", "Johnson", "bob@example.com", "Password123", "0987654321");
        otherUser.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(reviewRepository.findByIdAndUser(100L, otherUser)).thenReturn(Optional.empty());

        // 1. Cannot read
        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.getReviewById(2L, 100L)
        );

        // 2. Cannot update
        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.updateReview(2L, 100L, 3, "Malicious update")
        );

        // 3. Cannot delete
        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.deleteReview(2L, 100L)
        );

        verify(reviewRepository, never()).save(any());
        verify(reviewRepository, never()).delete(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {ReviewService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("reviews-by-product");
            }

            @Bean
            public ReviewRepository reviewRepository() {
                return Mockito.mock(ReviewRepository.class);
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
        private ReviewService reviewService;

        @Autowired
        private ReviewRepository reviewRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user;
        private Product product1;
        private Product product2;
        private Review review1;
        private Review review2;

        @BeforeEach
        void setUp() {
            Mockito.reset(reviewRepository, userRepository, productRepository);
            Cache cache = cacheManager.getCache("reviews-by-product");
            if (cache != null) {
                cache.clear();
            }

            user = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
            user.setId(1L);

            product1 = new Product("Smartphone", "Latest model", new BigDecimal("799.99"), 50, null);
            product1.setId(10L);

            product2 = new Product("Tablet", "Nice tablet", new BigDecimal("499.99"), 30, null);
            product2.setId(20L);

            review1 = new Review(user, product1, 5, "Great phone!");
            review1.setId(101L);

            review2 = new Review(user, product2, 4, "Good tablet!");
            review2.setId(102L);
        }

        @Test
        void testA_and_testB_firstLookup_and_cacheHit() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> page = new PageImpl<>(List.of(review1), pageable, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, pageable)).thenReturn(page);

            // Test A — First lookup (Cache Miss)
            Page<Review> first = reviewService.getReviewsByProduct(10L, pageable);
            assertNotNull(first);
            assertEquals(1, first.getTotalElements());
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);

            // Test B — Second lookup (Cache Hit)
            Page<Review> second = reviewService.getReviewsByProduct(10L, pageable);
            assertNotNull(second);
            assertEquals(1, second.getTotalElements());
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);
        }

        @Test
        void testC_productIsolation() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> page1 = new PageImpl<>(List.of(review1), pageable, 1);
            Page<Review> page2 = new PageImpl<>(List.of(review2), pageable, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(20L)).thenReturn(Optional.of(product2));
            when(reviewRepository.findByProduct(product1, pageable)).thenReturn(page1);
            when(reviewRepository.findByProduct(product2, pageable)).thenReturn(page2);

            Page<Review> r1 = reviewService.getReviewsByProduct(10L, pageable);
            assertEquals(101L, r1.getContent().get(0).getId());
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);

            Page<Review> r2 = reviewService.getReviewsByProduct(20L, pageable);
            assertEquals(102L, r2.getContent().get(0).getId());
            verify(reviewRepository, times(1)).findByProduct(product2, pageable);
        }

        @Test
        void testD_pageIsolation() {
            Pageable page0 = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Pageable page1 = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<Review> p0 = new PageImpl<>(List.of(review1), page0, 2);
            Page<Review> p1 = new PageImpl<>(List.of(review1), page1, 2);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, page0)).thenReturn(p0);
            when(reviewRepository.findByProduct(product1, page1)).thenReturn(p1);

            reviewService.getReviewsByProduct(10L, page0);
            verify(reviewRepository, times(1)).findByProduct(product1, page0);

            reviewService.getReviewsByProduct(10L, page1);
            verify(reviewRepository, times(1)).findByProduct(product1, page1);
        }

        @Test
        void testE_sortIsolation() {
            Pageable sortDesc = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Pageable sortAsc = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"));

            Page<Review> pDesc = new PageImpl<>(List.of(review1), sortDesc, 1);
            Page<Review> pAsc = new PageImpl<>(List.of(review1), sortAsc, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, sortDesc)).thenReturn(pDesc);
            when(reviewRepository.findByProduct(product1, sortAsc)).thenReturn(pAsc);

            reviewService.getReviewsByProduct(10L, sortDesc);
            verify(reviewRepository, times(1)).findByProduct(product1, sortDesc);

            reviewService.getReviewsByProduct(10L, sortAsc);
            verify(reviewRepository, times(1)).findByProduct(product1, sortAsc);
        }

        @Test
        void testF_createInvalidation() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> page = new PageImpl<>(List.of(review1), pageable, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, pageable)).thenReturn(page);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByUserAndProduct(user, product1)).thenReturn(Optional.empty());
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            // Populate cache
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);

            // Create review
            reviewService.createReview(1L, 10L, 5, "New review");

            // Next getReviewsByProduct should hit repository again
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(2)).findByProduct(product1, pageable);
        }

        @Test
        void testG_updateInvalidation() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> page = new PageImpl<>(List.of(review1), pageable, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, pageable)).thenReturn(page);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByIdAndUser(101L, user)).thenReturn(Optional.of(review1));
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            // Populate cache
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);

            // Update review
            reviewService.updateReview(1L, 101L, 4, "Updated review");

            // Next getReviewsByProduct should hit repository again
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(2)).findByProduct(product1, pageable);
        }

        @Test
        void testH_deleteInvalidation() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> page = new PageImpl<>(List.of(review1), pageable, 1);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
            when(reviewRepository.findByProduct(product1, pageable)).thenReturn(page);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByIdAndUser(101L, user)).thenReturn(Optional.of(review1));

            // Populate cache
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(1)).findByProduct(product1, pageable);

            // Delete review
            reviewService.deleteReview(1L, 101L);

            // Next getReviewsByProduct should hit repository again
            reviewService.getReviewsByProduct(10L, pageable);
            verify(reviewRepository, times(2)).findByProduct(product1, pageable);
        }
    }
}
