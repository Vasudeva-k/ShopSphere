package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Cart;
import com.vasudev.shopsphere.entity.CartItem;
import com.vasudev.shopsphere.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
