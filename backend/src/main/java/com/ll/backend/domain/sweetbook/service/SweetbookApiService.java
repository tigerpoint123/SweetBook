package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.dto.book.AddBookContentsRequest;
import com.ll.backend.global.client.dto.book.AddBookContentsResponse;
import com.ll.backend.global.client.dto.book.BookGalleryData;
import com.ll.backend.global.client.dto.book.BookPhotosData;
import com.ll.backend.global.client.dto.book.BooksListData;
import com.ll.backend.global.client.dto.book.CreateBookRequest;
import com.ll.backend.global.client.dto.book.CreateBookResponseData;
import com.ll.backend.global.client.dto.photo.PhotoUploadData;
import com.ll.backend.global.client.dto.book.SweetbookResponse;
import com.ll.backend.global.client.dto.book.SweetbookApiEnvelope;
import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface SweetbookApiService {

    SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo);

    SweetbookApiResponse<CreateBookResponseData> createBook(CreateBookRequest request, Optional<Long> memberId);

    List<MyBookItemResponse> listBooksOwnedByMember(Long memberId);

    SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid);

    SweetbookApiEnvelope<BookGalleryData> getBookPhotosAfterLocalLookup(String bookUid);

    SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file);

    SweetbookResponse uploadBookCover(
            String bookUid,
            String templateUid,
            String parametersJson,
            MultipartFile coverPhoto,
            MultipartFile backPhoto);

    AddBookContentsResponse addBookContents(String bookUid, AddBookContentsRequest request, Long memberId);

    SweetbookResponse deleteBook(String bookUid, Long memberId);

    /** Sweetbook DELETE /v1/books/{bookUid}/photos/{fileName} — 북 소유자만, 로컬 DB에 동일 fileName 행이 있을 때 */
    SweetbookResponse deleteBookPhoto(String bookUid, String fileName, Long memberId);

    /** Sweetbook POST /v1/books/{bookUid}/finalization — 북 소유자만. 성공 시 로컬 {@code sweetbook_book}에 {@code price} 반영. */
    SweetbookResponse finalizeBook(String bookUid, Long memberId, long price);
}
