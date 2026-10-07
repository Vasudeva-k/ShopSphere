package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.Review;
import com.vasudev.shopsphere.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndUser(Long id, User user);

    Optional<Review> findByUserAndProduct(User user, Product product);

    Page<Review> findByProduct(Product product, Pageable pageable);

    Page<Review> findByUser(User user, Pageable pageable);
}
