package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.dto.AddBookContentsRequest;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.BooksListData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface SweetbookApiService {

    SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo);

    Map<String, Object> createBook(CreateBookRequest request, Optional<Long> memberId);

    List<MyBookItemResponse> listBooksOwnedByMember(Long memberId);

    SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid);

    SweetbookApiEnvelope<BookPhotosData> getBookPhotosAfterLocalLookup(String bookUid);

    SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file);

    Map<String, Object> uploadBookCover(
            String bookUid,
            String templateUid,
            String parametersJson,
            MultipartFile coverPhoto,
            MultipartFile backPhoto);

    Map<String, Object> addBookContents(String bookUid, AddBookContentsRequest request, Long memberId);

    Map<String, Object> deleteBook(String bookUid, Long memberId);

    /** Sweetbook DELETE /v1/books/{bookUid}/photos/{fileName} — 북 소유자만, 로컬 DB에 동일 fileName 행이 있을 때 */
    Map<String, Object> deleteBookPhoto(String bookUid, String fileName, Long memberId);

    /** Sweetbook POST /v1/books/{bookUid}/finalization — 북 소유자만 */
    Map<String, Object> finalizeBook(String bookUid, Long memberId);
}
