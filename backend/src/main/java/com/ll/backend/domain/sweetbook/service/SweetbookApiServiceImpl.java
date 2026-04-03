package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.global.client.SweetbookApiClient;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.List;
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
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhotos(String bookUid, List<MultipartFile> files) {
        return sweetbookApiClient.uploadPhotos(bookUid, files);
    }
}
