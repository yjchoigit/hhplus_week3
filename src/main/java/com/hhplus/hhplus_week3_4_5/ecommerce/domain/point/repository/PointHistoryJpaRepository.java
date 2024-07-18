package com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.repository;

import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findAllByPoint_PointId(Long pointId);
}
