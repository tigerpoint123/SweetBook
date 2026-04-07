package com.ll.backend.domain.order.repository;

import com.ll.backend.domain.order.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long>, ShopOrderRepositoryCustom {

    long countByMemberId(Long memberId);
}
