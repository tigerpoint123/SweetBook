package com.ll.backend.domain.sweetbook.repository;

import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SweetbookBookRepository extends JpaRepository<SweetbookBook, Long> {

    boolean existsByBookUid(String bookUid);

    boolean existsByBookUidAndMemberId(String bookUid, Long memberId);

    Optional<SweetbookBook> findByBookUid(String bookUid);

    List<SweetbookBook> findAllByMemberIdOrderByIdDesc(Long memberId);

    void deleteByBookUidAndMemberId(String bookUid, Long memberId);
}
