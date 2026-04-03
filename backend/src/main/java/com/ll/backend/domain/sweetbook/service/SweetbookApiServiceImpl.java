package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.global.client.SweetbookApiClient;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SweetbookApiServiceImpl implements SweetbookApiService {

    private final SweetbookApiClient sweetbookApiClient;

    @Override
    public Map<String, Object> getBooks() {
        return sweetbookApiClient.getBooks();
    }

    @Override
    public Map<String, Object> createBook(CreateBookRequest request) {
        return sweetbookApiClient.createBook(request);
    }

    @Override
    public SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid) {
        return sweetbookApiClient.getBookPhotos(bookUid);
    }

    @Override
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file) {
        return sweetbookApiClient.uploadPhoto(bookUid, file);
    }
}
