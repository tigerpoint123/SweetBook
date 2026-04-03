package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface SweetbookApiService {

    Map<String, Object> getBooks();

    SweetbookApiEnvelope<PhotoUploadData> uploadPhotos(String bookUid, List<MultipartFile> files);
}
