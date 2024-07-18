package com.hhplus.hhplus_week3_4_5.ecommerce.domain.cart.repository;

import com.hhplus.hhplus_week3_4_5.ecommerce.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartJpaRepository extends JpaRepository<Cart, Long> {
    List<Cart> findAllByBuyerId(Long buyerId);
    List<Cart> findAllByBuyerIdAndCartIdIn(Long buyerId, List<Long> cartIdList);
}
