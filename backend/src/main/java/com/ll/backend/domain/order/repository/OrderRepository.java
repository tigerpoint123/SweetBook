package com.ll.backend.domain.order.repository;

import com.ll.backend.domain.order.entity.BookOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<BookOrder, Long> {

    boolean existsByMemberIdAndBookUid(Long memberId, String bookUid);
}
