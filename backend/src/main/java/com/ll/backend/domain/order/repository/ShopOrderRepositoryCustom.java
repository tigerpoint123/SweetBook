package com.ll.backend.domain.order.repository;

import com.ll.backend.domain.order.entity.ShopOrder;
import java.util.List;

public interface ShopOrderRepositoryCustom {

    List<ShopOrder> findByMemberIdOrderByCreatedAtDesc(long memberId, int limit, int offset);
}
