import { API_BASE } from "@/lib/api-config";

export type LocalPhotoItem = {
  id: number;
  bookUid: string;
  originalName: string;
  sweetbookFileName: string;
  size: number;
  mimeType: string;
  uploadedAt: string;
  hash: string;
  isDuplicate: boolean;
  fileUrl: string;
  isSample?: boolean;
  /** 책 최종화 시 설정된 단가(원). 사진별로 동일 값이 반복될 수 있음. */
  price?: number | null;
  originalUrl?: string;
};

export function localPhotoAbsoluteUrl(fileUrl: string): string {
  if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
    return fileUrl;
  }
  return `${API_BASE}${fileUrl.startsWith("/") ? "" : "/"}${fileUrl}`;
}

/** 파일명에 쓸 수 없는 문자 제거·축약 */
export function sanitizeDownloadFilename(name: string, fallback: string): string {
  const base = name?.trim() || fallback;
  const safe = base.replace(/[/\\?%*:|"<>]/g, "_").replace(/\s+/g, " ").trim();
  return safe.length > 200 ? safe.slice(0, 200) : safe;
}

/**
 * 원격(백엔드) 이미지 URL을 Blob으로 받아 브라우저 저장 대화상자로 내려받습니다.
 * 크로스 오리진에서는 단순 링크의 download 속성만으로는 불안정해 fetch 후 blob으로 저장합니다.
 */
export async function downloadLocalPhoto(
  fileUrl: string,
  suggestedName: string
): Promise<void> {
  const url = localPhotoAbsoluteUrl(fileUrl);
  const res = await fetch(url, { method: "GET", credentials: "include" });
  if (!res.ok) {
    throw new Error(`download failed: ${res.status}`);
  }
  const blob = await res.blob();
  const name = sanitizeDownloadFilename(
    suggestedName,
    url.split("/").pop() || "photo"
  );
  const extFromMime = (() => {
    const t = res.headers.get("Content-Type")?.split(";")[0]?.trim();
    if (t === "image/jpeg" || t === "image/jpg") return ".jpg";
    if (t === "image/png") return ".png";
    if (t === "image/gif") return ".gif";
    if (t === "image/webp") return ".webp";
    return "";
  })();
  const hasExt = /\.[a-z0-9]{2,8}$/i.test(name);
  const filename = hasExt ? name : name + extFromMime;

  const objectUrl = URL.createObjectURL(blob);
  try {
    const a = document.createElement("a");
    a.href = objectUrl;
    a.download = filename;
    a.rel = "noopener";
    document.body.appendChild(a);
    a.click();
    a.remove();
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

/** 샘플·비샘플 두 번 조회 후 id 내림차순으로 합칩니다(책 갤러리 왼쪽 목록용). */
export function mergeBookGalleryLocalPhotos(
  samples: LocalPhotoItem[],
  nonSamples: LocalPhotoItem[]
): LocalPhotoItem[] {
  const byId = new Map<number, LocalPhotoItem>();
  for (const p of samples) {
    if (typeof p?.id === "number") byId.set(p.id, p);
  }
  for (const p of nonSamples) {
    if (typeof p?.id === "number") byId.set(p.id, p);
  }
  return [...byId.values()].sort((a, b) => b.id - a.id);
}

/** GET …/sample-photos + …/non-sample-photos (병렬 2회) */
export async function fetchBookLocalPhotosSplit(bookUid: string): Promise<{
  ok: boolean;
  photos: LocalPhotoItem[];
  errorMessage: string | null;
}> {
  const uid = bookUid.trim();
  if (!uid) {
    return { ok: false, photos: [], errorMessage: "bookUid가 없습니다." };
  }
  const base = `${API_BASE}/api/photos/books/${encodeURIComponent(uid)}`;
  const [sampleRes, nonSampleRes] = await Promise.all([
    fetch(`${base}/sample-photos`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    }),
    fetch(`${base}/non-sample-photos`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    }),
  ]);
  const sampleText = await sampleRes.text();
  const nonSampleText = await nonSampleRes.text();
  if (!sampleRes.ok) {
    return {
      ok: false,
      photos: [],
      errorMessage: sampleText || `샘플 사진 목록 실패 (${sampleRes.status})`,
    };
  }
  if (!nonSampleRes.ok) {
    return {
      ok: false,
      photos: [],
      errorMessage: nonSampleText || `비샘플 사진 목록 실패 (${nonSampleRes.status})`,
    };
  }
  try {
    const samples = JSON.parse(sampleText) as unknown;
    const nonSamples = JSON.parse(nonSampleText) as unknown;
    if (!Array.isArray(samples) || !Array.isArray(nonSamples)) {
      return { ok: false, photos: [], errorMessage: "사진 목록 형식이 올바르지 않습니다." };
    }
    return {
      ok: true,
      photos: mergeBookGalleryLocalPhotos(
        samples as LocalPhotoItem[],
        nonSamples as LocalPhotoItem[]
      ),
      errorMessage: null,
    };
  } catch {
    return { ok: false, photos: [], errorMessage: "로컬 사진 목록을 해석할 수 없습니다." };
  }
}

/** GET /api/photos — 선택적 bookUid 시 해당 북만; 갤러리 순서는 photo.id 내림차순 */
export async function fetchLocalPhotos(bookUid?: string): Promise<Response> {
  const q = bookUid?.trim()
    ? `?bookUid=${encodeURIComponent(bookUid.trim())}`
    : "";
  return fetch(`${API_BASE}/api/photos${q}`, {
    method: "GET",
    credentials: "include",
  });
}

/** GET …/selected — 책 넘김용 채택만, selected_photo.id 오름차순(최근 채택이 뒤) */
export async function fetchSelectedPhotosForBook(bookUid: string): Promise<Response> {
  return fetch(
    `${API_BASE}/api/photos/books/${encodeURIComponent(bookUid.trim())}/selected`,
    { method: "GET", credentials: "include", cache: "no-store" }
  );
}

/** POST /api/photos/books/{bookUid}/selected — 이번 채택분만 추가(기존 유지) */
export async function appendBookSelection(
  bookUid: string,
  photoIds: number[]
): Promise<Response> {
  return fetch(
    `${API_BASE}/api/photos/books/${encodeURIComponent(bookUid.trim())}/selected`,
    {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ photoIds }),
    }
  );
}

export type BookCoverItem = {
  bookUid: string;
  photoId: number;
  fileUrl: string;
  subtitle: string;
  dateRange: string;
  updatedAt: string;
};

export async function fetchBookCovers(): Promise<Response> {
  return fetch(`${API_BASE}/api/photos/book-covers`, {
    method: "GET",
    credentials: "include",
  });
}

export async function saveBookCover(
  bookUid: string,
  body: { photoId: number; subtitle?: string; dateRange?: string }
): Promise<Response> {
  return fetch(
    `${API_BASE}/api/photos/books/${encodeURIComponent(bookUid)}/book-cover`,
    {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );
}
