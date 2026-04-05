package com.ll.backend.domain.photo.repository;

import com.ll.backend.domain.photo.entity.SelectedPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelectedPhotoRepository extends JpaRepository<SelectedPhoto, Long> {

    List<SelectedPhoto> findAllByBookUidOrderByIdAsc(String bookUid);

    void deleteByPhotoId(Long photoId);
}
