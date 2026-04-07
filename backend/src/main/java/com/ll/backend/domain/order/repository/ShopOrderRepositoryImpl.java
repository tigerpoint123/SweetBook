package com.ll.backend.domain.order.repository;

import com.ll.backend.domain.order.entity.ShopOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ShopOrderRepositoryImpl implements ShopOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ShopOrder> findByMemberIdOrderByCreatedAtDesc(long memberId, int limit, int offset) {
        return entityManager
                .createQuery(
                        "SELECT o FROM ShopOrder o WHERE o.memberId = :m ORDER BY o.createdAt DESC",
                        ShopOrder.class)
                .setParameter("m", memberId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
