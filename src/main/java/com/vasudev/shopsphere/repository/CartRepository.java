package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Cart;
import com.vasudev.shopsphere.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);
}
