package com.hhplus.hhplus_week3_4_5.ecommerce.domain.buyer.repository;

import com.hhplus.hhplus_week3_4_5.ecommerce.domain.buyer.entity.Buyer;
import org.springframework.stereotype.Repository;

@Repository
public class BuyerRepositoryImpl implements BuyerRepository {
    private final BuyerJpaRepository buyerJpaRepository;

    public BuyerRepositoryImpl(BuyerJpaRepository buyerJpaRepository) {
        this.buyerJpaRepository = buyerJpaRepository;
    }

    @Override
    public Buyer save(Buyer buyer) {
        return buyerJpaRepository.save(buyer);
    }
}
