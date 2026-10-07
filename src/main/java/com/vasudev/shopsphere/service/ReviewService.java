package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.Review;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.ReviewRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this(reviewRepository, userRepository, productRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CacheManager cacheManager
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public Review createReview(
            Long userId,
            Long productId,
            Integer rating,
            String comment
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (reviewRepository.findByUserAndProduct(user, product).isPresent()) {
            throw new IllegalStateException("User has already reviewed this product");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = new Review(user, product, rating, comment);
        Review saved = reviewRepository.save(review);
        evictProductReviews(productId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Review getReviewById(Long userId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "reviews-by-product", key = "#productId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<Review> getReviewsByProduct(
            Long productId,
            Pageable pageable
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        return reviewRepository.findByProduct(product, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Review> getReviewsByUser(
            Long userId,
            Pageable pageable
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return reviewRepository.findByUser(user, pageable);
    }

    @Transactional
    public Review updateReview(
            Long userId,
            Long reviewId,
            Integer rating,
            String comment
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        review.setRating(rating);
        review.setComment(comment);
        Review updated = reviewRepository.save(review);
        if (review.getProduct() != null && review.getProduct().getId() != null) {
            evictProductReviews(review.getProduct().getId());
        }
        return updated;
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        Long productId = (review.getProduct() != null) ? review.getProduct().getId() : null;
        reviewRepository.delete(review);
        if (productId != null) {
            evictProductReviews(productId);
        }
    }

    private void evictProductReviews(Long productId) {
        if (cacheManager == null || productId == null) {
            return;
        }
        Cache cache = cacheManager.getCache("reviews-by-product");
        if (cache == null) {
            return;
        }
        if (cache.getNativeCache() instanceof java.util.concurrent.ConcurrentMap<?, ?> map) {
            String prefix = productId + ":";
            for (Object key : map.keySet()) {
                if (key != null && key.toString().startsWith(prefix)) {
                    cache.evict(key);
                }
            }
        }
    }
}
