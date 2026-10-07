package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Cart;
import com.vasudev.shopsphere.entity.CartItem;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.CartItemRepository;
import com.vasudev.shopsphere.repository.CartRepository;
import com.vasudev.shopsphere.repository.OrderItemRepository;
import com.vasudev.shopsphere.repository.OrderRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createOrderFromCart(Long userId) {
        // STEP 1 — FIND USER
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // STEP 2 — FIND CART
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty or does not exist for user id: " + userId));

        // STEP 3 — LOAD CART ITEMS
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from an empty cart");
        }

        // STEP 4 — VALIDATE ALL PRODUCTS BEFORE MUTATION
        List<Product> validatedProducts = new ArrayList<>();
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                throw new ResourceNotFoundException("Product associated with cart item does not exist");
            }

            Product freshProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + product.getId()));

            if (!Boolean.TRUE.equals(freshProduct.getEnabled())) {
                throw new IllegalArgumentException("Product '" + freshProduct.getName() + "' is disabled and cannot be ordered");
            }

            if (freshProduct.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product '" + freshProduct.getName() + "'. Available: "
                                + freshProduct.getStockQuantity() + ", requested: " + item.getQuantity()
                );
            }

            validatedProducts.add(freshProduct);
        }

        // STEP 5 — CALCULATE TOTAL & DEDUCT STOCK
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            Product freshProduct = validatedProducts.get(i);

            freshProduct.setStockQuantity(freshProduct.getStockQuantity() - item.getQuantity());
            productRepository.save(freshProduct);

            BigDecimal unitPrice = freshProduct.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // STEP 6 — CREATE AND SAVE ORDER
        Order order = new Order(user, totalAmount, OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        // STEP 7 — CREATE AND SAVE ORDER ITEMS (HISTORICAL SNAPSHOTS)
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            Product freshProduct = validatedProducts.get(i);

            BigDecimal unitPrice = freshProduct.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    freshProduct,
                    freshProduct.getName(),
                    unitPrice,
                    item.getQuantity(),
                    subtotal
            );
            orderItemRepository.save(orderItem);
        }

        // STEP 8 — CLEAR CART
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#userId + ':' + #orderId")
    public Order getOrderByIdAndUser(Long orderId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return orderRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#userId + ':' + #orderId")
    public Order cancelOrder(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}


