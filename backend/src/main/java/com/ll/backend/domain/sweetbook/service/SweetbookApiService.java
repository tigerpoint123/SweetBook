package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface SweetbookApiService {

    Map<String, Object> getBooks();

    Map<String, Object> createBook(CreateBookRequest request);

    SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid);

    SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file);
}
