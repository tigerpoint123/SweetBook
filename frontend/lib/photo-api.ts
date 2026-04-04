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

/** GET /api/photos — 선택적 bookUid 쿼리로 해당 북만 필터 */
export async function fetchLocalPhotos(bookUid?: string): Promise<Response> {
  const q = bookUid?.trim()
    ? `?bookUid=${encodeURIComponent(bookUid.trim())}`
    : "";
  return fetch(`${API_BASE}/api/photos${q}`, {
    method: "GET",
    credentials: "include",
  });
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
