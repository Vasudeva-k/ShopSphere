package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.dto.cart.AddCartItemRequest;
import com.vasudev.shopsphere.dto.cart.CartItemResponse;
import com.vasudev.shopsphere.dto.cart.CartResponse;
import com.vasudev.shopsphere.dto.cart.UpdateCartItemRequest;
import com.vasudev.shopsphere.entity.Cart;
import com.vasudev.shopsphere.entity.CartItem;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.CartItemRepository;
import com.vasudev.shopsphere.repository.CartRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    @Cacheable(value = "cart", key = "#userId")
    public CartResponse getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!Boolean.TRUE.equals(product.getEnabled())) {
            throw new IllegalArgumentException("Product is disabled and cannot be added to cart");
        }

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Requested quantity (" + newQuantity + ") exceeds available stock (" + product.getStockQuantity() + ")"
                );
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + product.getStockQuantity() + ")"
                );
            }

            CartItem newItem = new CartItem(cart, product, request.getQuantity());
            cartItemRepository.save(newItem);
        }

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item does not belong to user's cart");
        }

        Product product = cartItem.getProduct();
        if (!Boolean.TRUE.equals(product.getEnabled())) {
            throw new IllegalArgumentException("Product is disabled and cannot be updated in cart");
        }

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        if (request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + product.getStockQuantity() + ")"
            );
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public CartResponse removeItem(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item does not belong to user's cart");
        }

        cartItemRepository.delete(cartItem);

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public CartResponse clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Cart> cartOpt = cartRepository.findByUser(user);
        if (cartOpt.isEmpty()) {
            return new CartResponse(null, userId, List.of(), BigDecimal.ZERO);
        }

        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCart(cart);
        cartItemRepository.deleteAll(items);

        return new CartResponse(cart.getId(), userId, List.of(), BigDecimal.ZERO);
    }

    private CartResponse toCartResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(
                    item.getId(),
                    product.getId(),
                    product.getName(),
                    unitPrice,
                    item.getQuantity(),
                    subtotal
            );
        }).toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), cart.getUser().getId(), itemResponses, total);
    }
}
