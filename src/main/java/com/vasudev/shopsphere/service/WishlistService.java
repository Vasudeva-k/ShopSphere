package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.entity.WishlistItem;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import com.vasudev.shopsphere.repository.WishlistItemRepository;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this(wishlistItemRepository, userRepository, productRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CacheManager cacheManager
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public WishlistItem addItem(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (wishlistItemRepository.findByUserAndProduct(user, product).isPresent()) {
            throw new IllegalStateException("Product is already in the wishlist");
        }

        WishlistItem item = new WishlistItem(user, product);
        WishlistItem saved = wishlistItemRepository.save(item);
        evictUserWishlist(userId);
        return saved;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "wishlist", key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<WishlistItem> getWishlist(
            Long userId,
            Pageable pageable
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return wishlistItemRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public WishlistItem getItem(
            Long userId,
            Long wishlistItemId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return wishlistItemRepository.findByIdAndUser(wishlistItemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistItemId));
    }

    @Transactional
    public void removeItem(
            Long userId,
            Long wishlistItemId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        WishlistItem item = wishlistItemRepository.findByIdAndUser(wishlistItemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistItemId));

        wishlistItemRepository.delete(item);
        evictUserWishlist(userId);
    }

    @Transactional(readOnly = true)
    public boolean isProductInWishlist(
            Long userId,
            Long productId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        return wishlistItemRepository.findByUserAndProduct(user, product).isPresent();
    }

    @Transactional
    public void clearWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<WishlistItem> items = wishlistItemRepository.findByUser(user);
        wishlistItemRepository.deleteAll(items);
        evictUserWishlist(userId);
    }

    private void evictUserWishlist(Long userId) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache("wishlist");
        if (cache == null) {
            return;
        }
        if (cache.getNativeCache() instanceof java.util.concurrent.ConcurrentMap<?, ?> map) {
            String prefix = userId + ":";
            for (Object key : map.keySet()) {
                if (key != null && key.toString().startsWith(prefix)) {
                    cache.evict(key);
                }
            }
        }
    }
}
