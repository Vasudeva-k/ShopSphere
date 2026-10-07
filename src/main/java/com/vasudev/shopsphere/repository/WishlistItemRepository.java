package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByIdAndUser(Long id, User user);

    Optional<WishlistItem> findByUserAndProduct(User user, Product product);

    Page<WishlistItem> findByUser(User user, Pageable pageable);

    List<WishlistItem> findByUser(User user);
}
