package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUser(Long id, User user);

    Page<Order> findByUser(User user, Pageable pageable);
}
