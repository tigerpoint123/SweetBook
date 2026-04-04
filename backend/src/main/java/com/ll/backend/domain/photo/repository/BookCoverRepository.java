package com.ll.backend.domain.photo.repository;

import com.ll.backend.domain.photo.entity.BookCover;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCoverRepository extends JpaRepository<BookCover, Long> {

    Optional<BookCover> findByBookUid(String bookUid);

    List<BookCover> findAllByOrderByUpdatedAtDesc();
}
