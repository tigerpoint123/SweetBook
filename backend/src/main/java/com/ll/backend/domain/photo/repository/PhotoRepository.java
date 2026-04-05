package com.ll.backend.domain.photo.repository;

import com.ll.backend.domain.photo.entity.Photo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findAllByOrderByIdDesc();

    List<Photo> findByBookUidOrderByIdDesc(String bookUid);

    List<Photo> findByBookUidOrderByIdAsc(String bookUid);

    boolean existsByBookUid(String bookUid);

    Optional<Photo> findByBookUidAndSweetbookFileName(String bookUid, String sweetbookFileName);

    @Query("SELECT p FROM Photo p WHERE p.bookUid = :bookUid AND p.sample = true ORDER BY p.id ASC")
    List<Photo> findSamplesByBookUidOrderByIdAsc(@Param("bookUid") String bookUid);

    @Query("SELECT p FROM Photo p WHERE p.bookUid = :bookUid AND p.sample = false ORDER BY p.id DESC")
    List<Photo> findNonSamplesByBookUidOrderByIdDesc(@Param("bookUid") String bookUid);
}
