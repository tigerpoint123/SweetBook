package com.ll.backend.domain.photo.service;

import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;

public interface PhotoService {

    record ServedPhoto(Resource resource, String contentType) {}

    List<LocalPhotoItemResponse> list(Optional<String> bookUid);

    ServedPhoto servePhoto(long id);

    List<BookCoverItemResponse> listBookCovers();

    void saveBookCover(String bookUid, SaveBookCoverRequest request);
}
