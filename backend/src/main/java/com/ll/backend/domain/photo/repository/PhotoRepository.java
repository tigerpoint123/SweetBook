package com.ll.backend.domain.photo.repository;

import com.ll.backend.domain.photo.entity.Photo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findAllByOrderByIdDesc();

    List<Photo> findByBookUidOrderByIdDesc(String bookUid);

    boolean existsByBookUid(String bookUid);

    Optional<Photo> findByBookUidAndSweetbookFileName(String bookUid, String sweetbookFileName);
}
