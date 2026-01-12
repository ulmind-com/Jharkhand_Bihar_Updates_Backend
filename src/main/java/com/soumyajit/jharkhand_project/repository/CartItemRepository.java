package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartSessionId(Long cartSessionId);

    Optional<CartItem> findByCartSessionIdAndProductId(Long cartSessionId, Long productId);

    void deleteByCartSessionId(Long cartSessionId);

    // CartItemRepository.java ke andar yeh line add kar
    void deleteAllByProductId(Long productId);

    // Inside CartItemRepository.java
    boolean existsByCartSessionId(Long cartSessionId);
}
