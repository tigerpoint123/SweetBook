package com.ll.backend.bootstrap;

import com.ll.backend.domain.member.repository.MemberRepository;
import com.ll.backend.domain.member.service.MemberService;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import com.ll.backend.global.client.dto.book.CreateBookRequest;
import com.ll.backend.domain.sweetbook.service.SweetbookApiService;
import com.ll.backend.domain.sweetbook.support.SweetbookCreateResponseParser;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * 서버 기동 시
 * 1) 회원가입: test / test
 * 2) 책 생성: title=test, author=사용자
 * 3) uploads/dummy 폴더의 이미지들을 해당 책에 업로드
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DummyScenarioInitializer implements ApplicationRunner {

    @Value("${app.dummy.enabled:true}")
    private boolean enabled;

    @Value("${app.dummy.upload-dir:uploads/dummy}")
    private String dummyUploadDir;

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final SweetbookApiService sweetbookApiService;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("DummyScenarioInitializer 스킵: app.dummy.enabled=false");
            return;
        }

        seedMember("test", "test");
        MemberLoginResult login = memberService.login("test", "test");
        if (login == null || !login.success() || login.sessionId() == null || login.sessionId().isBlank()) {
            log.warn("DummyScenarioInitializer 중단: test 로그인 실패");
            return;
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(login.sessionId());
        if (memberIdOpt.isEmpty()) {
            log.warn("DummyScenarioInitializer 중단: 세션에서 memberId를 찾을 수 없습니다.");
            return;
        }

        String bookUid = createDummyBook(memberIdOpt);
        if (bookUid == null || bookUid.isBlank()) {
            log.warn("DummyScenarioInitializer 중단: bookUid 추출 실패");
            return;
        }

        uploadDummyPhotos(bookUid);
    }

    private void seedMember(String username, String password) {
        if (memberRepository.findByUsername(username).isPresent()) return;
        memberService.postMember(username, password);
    }

    private String createDummyBook(Optional<Long> memberIdOpt) {
        // bookSpecUid/externalRef는 기존 예시값 사용
        CreateBookRequest req = new CreateBookRequest(
                "test",
                "SQUAREBOOK_HC",
                "사용자",
                "partner-book-001"
        );
        var resp = sweetbookApiService.createBook(req, memberIdOpt);
        var data = resp != null ? resp.data() : null;
        return SweetbookCreateResponseParser.extractBookUid(data).orElse(null);
    }

    private void uploadDummyPhotos(String bookUid) {
        Path root = Paths.get(dummyUploadDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.info("DummyScenarioInitializer 업로드 스킵: dummy 폴더가 없습니다. path={}", root);
            return;
        }
        int ok = 0;
        int fail = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path f : ds) {
                if (!Files.isRegularFile(f)) continue;
                String name = f.getFileName().toString();
                if (!looksLikeImage(name)) continue;
                try {
                    byte[] bytes = Files.readAllBytes(f);
                    MultipartFile mf = new ByteArrayMultipartFile(
                            "file", name, probeOrGuessMimeType(f, name), bytes);
                    sweetbookApiService.uploadPhoto(bookUid, mf);
                    ok++;
                } catch (Exception e) {
                    fail++;
                    log.warn("DummyScenarioInitializer 더미 업로드 실패 file={} err={}", f, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("DummyScenarioInitializer dummy 폴더 읽기 실패 path={}", root, e);
        }
        log.info("DummyScenarioInitializer 업로드 완료 bookUid={} ok={} fail={} dir={}", bookUid, ok, fail, root);
    }

    private static boolean looksLikeImage(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".webp");
    }

    private static String probeOrGuessMimeType(Path f, String name) {
        try {
            String t = Files.probeContentType(f);
            if (t != null && !t.isBlank()) return t;
        } catch (IOException ignored) {
            // fall through
        }
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (n.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (n.endsWith(".webp")) return "image/webp";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes != null ? bytes : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }
    }
}

