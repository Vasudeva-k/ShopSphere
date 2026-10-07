package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, userRepository, productRepository);

        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testCart = new Cart(testUser);
        testCart.setId(10L);

        testProduct = new Product("Wireless Mouse", "Ergonomic mouse", new BigDecimal("25.00"), 50, null);
        testProduct.setId(100L);
        testProduct.setEnabled(true);
    }

    @Test
    void getOrCreateCart_whenCartExists_returnsCartResponseWithItemsAndTotal() {
        CartItem item1 = new CartItem(testCart, testProduct, 2);
        item1.setId(101L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item1));

        CartResponse response = cartService.getOrCreateCart(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());

        CartItemResponse itemResponse = response.getItems().get(0);
        assertEquals(101L, itemResponse.getId());
        assertEquals(100L, itemResponse.getProductId());
        assertEquals("Wireless Mouse", itemResponse.getProductName());
        assertEquals(new BigDecimal("25.00"), itemResponse.getUnitPrice());
        assertEquals(2, itemResponse.getQuantity());
        assertEquals(new BigDecimal("50.00"), itemResponse.getSubtotal());
        assertEquals(new BigDecimal("50.00"), response.getTotal());
    }

    @Test
    void getOrCreateCart_whenCartDoesNotExist_createsCartAndReturnsEmptyResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(cartItemRepository.findByCart(any(Cart.class))).thenReturn(List.of());

        CartResponse response = cartService.getOrCreateCart(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getOrCreateCart_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.getOrCreateCart(999L));
    }

    @Test
    void addItem_whenItemNotInCart_createsCartItemSuccessfully() {
        AddCartItemRequest request = new AddCartItemRequest(100L, 3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());

        CartItem savedItem = new CartItem(testCart, testProduct, 3);
        savedItem.setId(201L);
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(savedItem));

        CartResponse response = cartService.addItem(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(3, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("75.00"), response.getTotal());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getQuantity());
    }

    @Test
    void addItem_whenItemAlreadyInCart_incrementsQuantity() {
        AddCartItemRequest request = new AddCartItemRequest(100L, 2);
        CartItem existingItem = new CartItem(testCart, testProduct, 3);
        existingItem.setId(201L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(existingItem));

        CartResponse response = cartService.addItem(1L, request);

        assertNotNull(response);
        assertEquals(5, existingItem.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItem_whenProductNotFound_throwsResourceNotFoundException() {
        AddCartItemRequest request = new AddCartItemRequest(999L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addItem(1L, request));
    }

    @Test
    void addItem_whenProductDisabled_throwsIllegalArgumentException() {
        testProduct.setEnabled(false);
        AddCartItemRequest request = new AddCartItemRequest(100L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(1L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenRequestedQuantityExceedsStock_throwsIllegalArgumentException() {
        testProduct.setStockQuantity(5);
        AddCartItemRequest request = new AddCartItemRequest(100L, 6);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(1L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenCombinedQuantityExceedsStock_throwsIllegalArgumentException() {
        testProduct.setStockQuantity(5);
        CartItem existingItem = new CartItem(testCart, testProduct, 4);

        AddCartItemRequest request = new AddCartItemRequest(100L, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(existingItem));

        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(1L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItem_whenValid_updatesQuantity() {
        CartItem item = new CartItem(testCart, testProduct, 2);
        item.setId(50L);
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of(item));

        CartResponse response = cartService.updateItem(1L, 50L, request);

        assertNotNull(response);
        assertEquals(5, item.getQuantity());
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateItem_whenCartItemNotFound_throwsResourceNotFoundException() {
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateItem(1L, 999L, request));
    }

    @Test
    void updateItem_whenItemBelongsToAnotherCart_throwsResourceNotFoundException() {
        Cart otherCart = new Cart();
        otherCart.setId(99L);
        CartItem item = new CartItem(otherCart, testProduct, 2);
        item.setId(50L);

        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateItem(1L, 50L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItem_whenQuantityExceedsStock_throwsIllegalArgumentException() {
        testProduct.setStockQuantity(3);
        CartItem item = new CartItem(testCart, testProduct, 2);
        item.setId(50L);
        UpdateCartItemRequest request = new UpdateCartItemRequest(10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> cartService.updateItem(1L, 50L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void removeItem_whenValid_deletesItemAndReturnsCart() {
        CartItem item = new CartItem(testCart, testProduct, 2);
        item.setId(50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCart(testCart)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(1L, 50L);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_whenItemBelongsToAnotherCart_throwsResourceNotFoundException() {
        Cart otherCart = new Cart();
        otherCart.setId(99L);
        CartItem item = new CartItem(otherCart, testProduct, 2);
        item.setId(50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));

        assertThrows(ResourceNotFoundException.class, () -> cartService.removeItem(1L, 50L));
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void clearCart_whenCartExists_deletesAllItems() {
        CartItem item1 = new CartItem(testCart, testProduct, 2);
        CartItem item2 = new CartItem(testCart, testProduct, 1);
        List<CartItem> items = new ArrayList<>(List.of(item1, item2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart(testCart)).thenReturn(items);

        CartResponse response = cartService.clearCart(1L);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        verify(cartItemRepository).deleteAll(items);
    }

    @Test
    void clearCart_whenCartDoesNotExist_returnsEmptyCartResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

        CartResponse response = cartService.clearCart(1L);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {CartService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("cart");
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
        private CartService cartService;

        @Autowired
        private CartRepository cartRepository;

        @Autowired
        private CartItemRepository cartItemRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user1;
        private User user2;
        private Cart cart1;
        private Cart cart2;
        private Product product;

        @BeforeEach
        void setUp() {
            Mockito.reset(cartRepository, cartItemRepository, userRepository, productRepository);
            Cache cache = cacheManager.getCache("cart");
            if (cache != null) {
                cache.clear();
            }

            user1 = new User("User1", "One", "user1@example.com", "Password123", "1111111111");
            user1.setId(1L);

            user2 = new User("User2", "Two", "user2@example.com", "Password123", "2222222222");
            user2.setId(2L);

            cart1 = new Cart(user1);
            cart1.setId(10L);

            cart2 = new Cart(user2);
            cart2.setId(20L);

            product = new Product("Headphones", "Noise cancelling", new BigDecimal("100.00"), 50, null);
            product.setId(500L);
            product.setEnabled(true);
        }

        @Test
        void testA_and_testB_firstLookupObtainsFromRepository_secondLookupUsesCache() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of());

            // First lookup - Cache Miss
            CartResponse firstResponse = cartService.getOrCreateCart(1L);
            assertNotNull(firstResponse);
            assertEquals(10L, firstResponse.getId());
            assertEquals(1L, firstResponse.getUserId());
            verify(cartRepository, times(1)).findByUser(user1);

            // Second lookup - Cache Hit
            CartResponse secondResponse = cartService.getOrCreateCart(1L);
            assertNotNull(secondResponse);
            assertEquals(10L, secondResponse.getId());
            assertEquals(1L, secondResponse.getUserId());
            verify(cartRepository, times(1)).findByUser(user1);
        }

        @Test
        void testC_userIsolation_independentCacheEntries() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartRepository.findByUser(user2)).thenReturn(Optional.of(cart2));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of());
            when(cartItemRepository.findByCart(cart2)).thenReturn(List.of());

            // Populate user1 cart
            CartResponse response1 = cartService.getOrCreateCart(1L);
            assertEquals(10L, response1.getId());
            verify(cartRepository, times(1)).findByUser(user1);

            // Populate user2 cart
            CartResponse response2 = cartService.getOrCreateCart(2L);
            assertEquals(20L, response2.getId());
            verify(cartRepository, times(1)).findByUser(user2);

            // Evict user1 cart via clearCart
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of());
            cartService.clearCart(1L);

            // user1 should miss cache, user2 should still hit cache
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(3)).findByUser(user1); // 1 for initial lookup, 1 during clearCart, 1 after eviction

            cartService.getOrCreateCart(2L);
            verify(cartRepository, times(1)).findByUser(user2); // user2 remains cached (1 lookup only)
        }

        @Test
        void testD_addItemEviction_evictsUserCart() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of());
            when(productRepository.findById(500L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartAndProduct(cart1, product)).thenReturn(Optional.empty());

            // Populate cache
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(1)).findByUser(user1);

            // Add item
            AddCartItemRequest request = new AddCartItemRequest(500L, 2);
            cartService.addItem(1L, request);

            // Subsequent getOrCreateCart should hit repository again
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(3)).findByUser(user1); // 1 for initial lookup, 1 during addItem, 1 after eviction
        }

        @Test
        void testE_updateItemEviction_evictsUserCart() {
            CartItem cartItem = new CartItem(cart1, product, 1);
            cartItem.setId(1001L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of(cartItem));
            when(cartItemRepository.findById(1001L)).thenReturn(Optional.of(cartItem));

            // Populate cache
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(1)).findByUser(user1);

            // Update item
            UpdateCartItemRequest request = new UpdateCartItemRequest(3);
            cartService.updateItem(1L, 1001L, request);

            // Subsequent getOrCreateCart should hit repository again
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(3)).findByUser(user1);
        }

        @Test
        void testF_removeItemEviction_evictsUserCart() {
            CartItem cartItem = new CartItem(cart1, product, 1);
            cartItem.setId(1001L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of(cartItem));
            when(cartItemRepository.findById(1001L)).thenReturn(Optional.of(cartItem));

            // Populate cache
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(1)).findByUser(user1);

            // Remove item
            cartService.removeItem(1L, 1001L);

            // Subsequent getOrCreateCart should hit repository again
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(3)).findByUser(user1);
        }

        @Test
        void testG_clearCartEviction_evictsUserCart() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(cartRepository.findByUser(user1)).thenReturn(Optional.of(cart1));
            when(cartItemRepository.findByCart(cart1)).thenReturn(List.of());

            // Populate cache
            cartService.getOrCreateCart(1L);
            verify(cartRepository, times(1)).findByUser(user1);

            Cache cache = cacheManager.getCache("cart");
            assertNotNull(cache);
            assertNotNull(cache.get(1L));

            // Clear cart
            cartService.clearCart(1L);

            // Cache entry should be null
            assertNull(cache.get(1L));
        }
    }
}
