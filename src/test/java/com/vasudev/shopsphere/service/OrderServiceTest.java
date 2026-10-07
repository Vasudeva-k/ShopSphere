package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                cartRepository,
                cartItemRepository,
                userRepository,
                productRepository
        );

        testUser = new User("John", "Doe", "john@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testCart = new Cart(testUser);
        testCart.setId(10L);

        product1 = new Product("Laptop", "Tech laptop", new BigDecimal("1299.99"), 10, null);
        product1.setId(101L);
        product1.setEnabled(true);

        product2 = new Product("Wireless Mouse", "Ergonomic mouse", new BigDecimal("49.50"), 20, null);
        product2.setId(102L);
        product2.setEnabled(true);
    }

    // 1. Successful checkout
    @Test
    void createOrderFromCart_successfulCheckout() {
        CartItem item1 = new CartItem(testCart, product1, 1);
        item1.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(500L);
            return o;
        });

        Order createdOrder = orderService.createOrderFromCart(1L);

        assertNotNull(createdOrder);
        assertEquals(500L, createdOrder.getId());
        assertEquals(OrderStatus.PENDING, createdOrder.getStatus());
        assertEquals(new BigDecimal("1299.99"), createdOrder.getTotalAmount());
    }

    // 2. User not found -> ResourceNotFoundException
    @Test
    void createOrderFromCart_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrderFromCart(999L));
        verify(orderRepository, never()).save(any());
    }

    // 3. Cart not found -> IllegalArgumentException
    @Test
    void createOrderFromCart_whenCartNotFound_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));
        verify(orderRepository, never()).save(any());
    }

    // 4. Empty cart -> IllegalArgumentException
    @Test
    void createOrderFromCart_whenCartIsEmpty_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));
        verify(orderRepository, never()).save(any());
    }

    // 5. Disabled product -> IllegalArgumentException
    @Test
    void createOrderFromCart_whenProductDisabled_throwsIllegalArgumentException() {
        product1.setEnabled(false);
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));
    }

    // 6. Insufficient stock -> IllegalArgumentException
    @Test
    void createOrderFromCart_whenInsufficientStock_throwsIllegalArgumentException() {
        product1.setStockQuantity(2);
        CartItem item1 = new CartItem(testCart, product1, 5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));
    }

    // 7. Correct order total calculation using BigDecimal
    @Test
    void createOrderFromCart_correctOrderTotalCalculationUsingBigDecimal() {
        CartItem item1 = new CartItem(testCart, product1, 2); // 2 * 1299.99 = 2599.98
        CartItem item2 = new CartItem(testCart, product2, 3); // 3 * 49.50 = 148.50
        // Expected total = 2599.98 + 148.50 = 2748.48

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1, item2));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrderFromCart(1L);

        assertEquals(new BigDecimal("2748.48"), order.getTotalAmount());
    }

    // 8. Correct OrderItem product-name snapshot
    @Test
    void createOrderFromCart_createsCorrectOrderItemProductNameSnapshot() {
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertEquals("Laptop", captor.getValue().getProductName());
    }

    // 9. Correct OrderItem unit-price snapshot
    @Test
    void createOrderFromCart_createsCorrectOrderItemUnitPriceSnapshot() {
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertEquals(new BigDecimal("1299.99"), captor.getValue().getUnitPrice());
    }

    // 10. Correct OrderItem quantity and subtotal
    @Test
    void createOrderFromCart_createsCorrectOrderItemQuantityAndSubtotal() {
        CartItem item1 = new CartItem(testCart, product1, 3); // 3 * 1299.99 = 3899.97

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getQuantity());
        assertEquals(new BigDecimal("3899.97"), captor.getValue().getSubtotal());
    }

    // 11. Correct product stock deduction
    @Test
    void createOrderFromCart_deductsProductStockCorrectly() {
        CartItem item1 = new CartItem(testCart, product1, 4); // Initial stock: 10 -> remaining: 6

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        assertEquals(6, product1.getStockQuantity());
        verify(productRepository).save(product1);
    }

    // 12. Cart items are deleted after successful checkout
    @Test
    void createOrderFromCart_deletesCartItemsAfterSuccessfulCheckout() {
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        verify(cartItemRepository).deleteAll(List.of(item1));
    }

    // 13. Multiple cart items produce the correct total and multiple OrderItems
    @Test
    void createOrderFromCart_withMultipleCartItems_producesCorrectTotalAndMultipleOrderItems() {
        CartItem item1 = new CartItem(testCart, product1, 2);
        CartItem item2 = new CartItem(testCart, product2, 4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1, item2));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(1L);

        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        assertEquals(8, product1.getStockQuantity());
        assertEquals(16, product2.getStockQuantity());
    }

    // 14. Validation failure must not save an Order
    @Test
    void createOrderFromCart_validationFailure_doesNotSaveOrder() {
        product1.setEnabled(false);
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));
        verify(orderRepository, never()).save(any());
    }

    // 15. Validation failure must not save/update Products
    @Test
    void createOrderFromCart_validationFailure_doesNotMutateOrSaveProducts() {
        product1.setStockQuantity(2);
        CartItem item1 = new CartItem(testCart, product1, 5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));

        assertEquals(2, product1.getStockQuantity());
        verify(productRepository, never()).save(any());
    }

    // 16. Validation failure must not delete CartItems
    @Test
    void createOrderFromCart_validationFailure_doesNotDeleteCartItems() {
        product1.setEnabled(false);
        CartItem item1 = new CartItem(testCart, product1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrderFromCart(1L));

        verify(cartItemRepository, never()).deleteAll(any());
    }

    // 17. Existing order belonging to the user is returned
    @Test
    void getOrderByIdAndUser_whenOrderBelongsToUser_returnsOrder() {
        Order order = new Order(testUser, new BigDecimal("100.00"));
        order.setId(50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(50L, testUser)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByIdAndUser(50L, 1L);

        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    // 18. Order belonging to another user / not found through findByIdAndUser -> ResourceNotFoundException
    @Test
    void getOrderByIdAndUser_whenNotFoundOrBelongsToAnotherUser_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderByIdAndUser(999L, 1L));
    }

    // 19. Successful paginated user-order retrieval
    @Test
    void getOrdersByUser_successfulPaginatedUserOrderRetrieval() {
        Order order1 = new Order(testUser, new BigDecimal("100.00"));
        order1.setId(10L);
        Order order2 = new Order(testUser, new BigDecimal("200.00"));
        order2.setId(20L);
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = new PageImpl<>(List.of(order1, order2), pageable, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser, pageable)).thenReturn(page);

        Page<Order> result = orderService.getOrdersByUser(1L, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());
        assertEquals(20L, result.getContent().get(1).getId());
    }

    // 20. Invalid/null user handling if user not found -> ResourceNotFoundException
    @Test
    void getOrdersByUser_whenUserNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrdersByUser(999L, pageable));
        verify(orderRepository, never()).findByUser(any(), any());
    }

    // 21. Pageable information is passed correctly to the repository
    @Test
    void getOrdersByUser_passesPageableInformationCorrectlyToRepository() {
        Pageable pageable = PageRequest.of(2, 15, Sort.by(Sort.Direction.ASC, "totalAmount"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(eq(testUser), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getOrdersByUser(1L, pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUser(eq(testUser), captor.capture());

        Pageable captured = captor.getValue();
        assertEquals(2, captured.getPageNumber());
        assertEquals(15, captured.getPageSize());
        assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("totalAmount").getDirection());
    }

    // 22. Existing order items are returned from OrderItemRepository
    @Test
    void getOrderItems_returnsItemsFromOrderItemRepository() {
        Order order = new Order(testUser, new BigDecimal("1299.99"));
        order.setId(50L);

        OrderItem orderItem = new OrderItem(
                order,
                product1,
                "Laptop",
                new BigDecimal("1299.99"),
                1,
                new BigDecimal("1299.99")
        );

        when(orderItemRepository.findByOrder(order)).thenReturn(List.of(orderItem));

        List<OrderItem> items = orderService.getOrderItems(order);

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Laptop", items.get(0).getProductName());
        assertEquals(new BigDecimal("1299.99"), items.get(0).getUnitPrice());
    }

    @Nested
    @SpringJUnitConfig(classes = {OrderService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("orders");
            }

            @Bean
            public OrderRepository orderRepository() {
                return Mockito.mock(OrderRepository.class);
            }

            @Bean
            public OrderItemRepository orderItemRepository() {
                return Mockito.mock(OrderItemRepository.class);
            }

            @Bean
            public CartRepository cartRepository() {
                return Mockito.mock(CartRepository.class);
            }

            @Bean
            public CartItemRepository cartItemRepository() {
                return Mockito.mock(CartItemRepository.class);
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
        private OrderService orderService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user1;
        private User user2;
        private Order order1;
        private Order order2;

        @BeforeEach
        void setUp() {
            Mockito.reset(orderRepository, userRepository);
            Cache cache = cacheManager.getCache("orders");
            if (cache != null) {
                cache.clear();
            }

            user1 = new User("User1", "One", "user1@example.com", "Password123", "1111111111");
            user1.setId(1L);

            user2 = new User("User2", "Two", "user2@example.com", "Password123", "2222222222");
            user2.setId(2L);

            order1 = new Order(user1, new BigDecimal("100.00"), OrderStatus.PENDING);
            order1.setId(10L);

            order2 = new Order(user2, new BigDecimal("200.00"), OrderStatus.PENDING);
            order2.setId(10L);
        }

        @Test
        void testA_and_testB_firstLookupObtainsFromRepository_secondLookupUsesCache() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));

            // Test A — First lookup (Cache Miss)
            Order first = orderService.getOrderByIdAndUser(10L, 1L);
            assertNotNull(first);
            assertEquals(10L, first.getId());
            assertEquals(OrderStatus.PENDING, first.getStatus());
            verify(orderRepository, times(1)).findByIdAndUser(10L, user1);

            // Test B — Second lookup (Cache Hit)
            Order second = orderService.getOrderByIdAndUser(10L, 1L);
            assertNotNull(second);
            assertEquals(10L, second.getId());
            verify(orderRepository, times(1)).findByIdAndUser(10L, user1);
        }

        @Test
        void testC_userIsolation_independentCacheKeys() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));
            when(orderRepository.findByIdAndUser(10L, user2)).thenReturn(Optional.of(order2));

            // Lookup for user1/order10
            Order o1 = orderService.getOrderByIdAndUser(10L, 1L);
            assertEquals(user1, o1.getUser());
            verify(orderRepository, times(1)).findByIdAndUser(10L, user1);

            // Lookup for user2/order10 (same orderId, different user)
            Order o2 = orderService.getOrderByIdAndUser(10L, 2L);
            assertEquals(user2, o2.getUser());
            verify(orderRepository, times(1)).findByIdAndUser(10L, user2);

            Cache cache = cacheManager.getCache("orders");
            assertNotNull(cache);
            assertNotNull(cache.get("1:10"));
            assertNotNull(cache.get("2:10"));
        }

        @Test
        void testD_cancellationEviction_evictsOrderCacheEntry() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Populate cache
            orderService.getOrderByIdAndUser(10L, 1L);
            verify(orderRepository, times(1)).findByIdAndUser(10L, user1);

            Cache cache = cacheManager.getCache("orders");
            assertNotNull(cache);
            assertNotNull(cache.get("1:10"));

            // Cancel order
            orderService.cancelOrder(1L, 10L);

            // Verify cache entry is evicted
            assertNull(cache.get("1:10"));

            // Next lookup should hit repository again
            orderService.getOrderByIdAndUser(10L, 1L);
            verify(orderRepository, times(3)).findByIdAndUser(10L, user1); // 1 for initial lookup, 1 during cancelOrder, 1 after eviction
        }
    }
}
