package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);
}
