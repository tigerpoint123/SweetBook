import axios, { type AxiosResponse } from "axios";
import { API_BASE } from "@/lib/api-config";
import type { LocalPhotoItem } from "@/lib/photo-api";
import { localPhotoAbsoluteUrl } from "@/lib/photo-api";

export const sweetbookBooksUrl = `${API_BASE}/api/sweetbook/books`;

export function sweetbookPhotoUploadUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/photos`;
}

/** GET /v1/books/{bookUid}/photos 프록시 (DB 검증 없음) */
export function sweetbookBookPhotosUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/photos`;
}

/** 로컬 DB(sweetbook_book 또는 photo)에 북이 있을 때만 Sweetbook 사진 목록 */
export function sweetbookBookGalleryUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/gallery`;
}

/** POST /api/sweetbook/books/{bookUid}/cover → Sweetbook 표지 설정 */
export function sweetbookBookCoverUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/cover`;
}

/** POST /api/sweetbook/books/{bookUid}/contents → Sweetbook 책 콘텐츠 추가 */
export function sweetbookBookContentsUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/contents`;
}

export type AddBookContentsResponse = {
  success?: boolean;
  message?: string;
  data?: {
    result?: string;
    breakBefore?: string;
    pageCount?: number;
  };
};

export async function postBookContents(
  bookUid: string,
  body: { rowPhotos: string[]; monthYearLabel?: string }
): Promise<Response> {
  return fetch(sweetbookBookContentsUrl(bookUid), {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

/** 기본 표지 템플릿 UID — 백엔드 `SweetbookCoverDefaults`와 동일 */
export const SWEETBOOK_DEFAULT_COVER_TEMPLATE_UID = "1Es0DP4oARn8";

export type UploadBookCoverResponse = {
  success?: boolean;
  message?: string;
  data?: { result?: string };
};

/**
 * Sweetbook POST /v1/books/{bookUid}/cover 프록시.
 * multipart: templateUid, parameters(JSON), coverPhoto(필수). backPhoto는 options.backFile 있을 때만(중복 키 오류 방지).
 */
export async function uploadBookCover(
  bookUid: string,
  coverFile: File,
  options?: {
    backFile?: File;
    templateUid?: string;
    parameters?: Record<string, string>;
  }
): Promise<Response> {
  const formData = new FormData();
  formData.append(
    "templateUid",
    options?.templateUid ?? SWEETBOOK_DEFAULT_COVER_TEMPLATE_UID
  );
  const defaults: Record<string, string> = {
    title: "",
    author: "",
    subtitle: "",
    dateRange: "",
  };
  const params = { ...defaults, ...(options?.parameters ?? {}) };
  formData.append("parameters", JSON.stringify(params));
  formData.append("coverPhoto", coverFile);
  if (options?.backFile) {
    formData.append("backPhoto", options.backFile);
  }
  return fetch(sweetbookBookCoverUrl(bookUid), {
    method: "POST",
    credentials: "include",
    body: formData,
  });
}

export type BookPhotoItem = {
  fileName: string;
  originalName: string;
  size: number;
  mimeType: string;
  uploadedAt: string;
  hash: string;
};

export type BookPhotosData = {
  photos: BookPhotoItem[];
  totalCount: number;
};

export type BookPhotosEnvelope = {
  success: boolean;
  data: BookPhotosData;
};

export interface CreateBookPayload {
  title: string;
  bookSpecUid: string;
  bookAuthor: string;
  externalRef: string;
}

/** DELETE /api/sweetbook/books/{bookUid} → Sweetbook 책 삭제 */
export function sweetbookBookItemUrl(bookUid: string): string {
  return `${sweetbookBooksUrl}/${encodeURIComponent(bookUid)}`;
}

export type DeleteBookResponse = {
  success?: boolean;
  message?: string;
  data?: { bookUid?: string; status?: number };
};

export async function deleteSweetbookBook(bookUid: string): Promise<Response> {
  return fetch(sweetbookBookItemUrl(bookUid), {
    method: "DELETE",
    credentials: "include",
  });
}

export async function createBook(payload: CreateBookPayload): Promise<Response> {
  return fetch(sweetbookBooksUrl, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export type BookListItem = {
  bookUid: string;
  title: string;
  bookSpecUid: string;
  status: string | number;
  pdfStatus: string | number;
  pdfRequestedAt: string | null;
  createdAt: string;
  externalRef: string;
};

export type BooksListData = {
  books: BookListItem[];
  total: number | null;
  limit: number | null;
  offset: number | null;
};

export type BooksListEnvelope = {
  success: boolean;
  data: BooksListData;
};

export type BooksListQuery = {
  limit?: number;
  offset?: number;
  pdfStatusIn?: string;
  createdFrom?: string;
  createdTo?: string;
};

/** GET /api/sweetbook/books → Sweetbook GET /v1/books (쿼리는 값이 있을 때만 전달) */
/** 로그인 사용자가 이 백엔드에서 생성한 책(bookUid) 목록. SESSION 쿠키 필요. */
export type MyBookEntry = {
  bookUid: string;
  createdAt: string;
};

export async function fetchMyBookEntries(): Promise<Response> {
  return fetch(`${API_BASE}/api/sweetbook/my-books`, {
    method: "GET",
    credentials: "include",
  });
}

/**
 * DB에 저장된「내 책」순서를 유지하면서 Sweetbook 목록 메타(제목 등)를 붙입니다.
 * 목록 API 실패 시 bookUid만으로 표지(로컬 첫 사진 등)는 유지됩니다.
 */
export function mergeMyBooksWithSweetbookList(
  myEntries: MyBookEntry[],
  envelope: BooksListEnvelope | null
): BookListItem[] {
  const allBooks = envelope?.success ? (envelope.data?.books ?? []) : [];
  const map = new Map(allBooks.map((b) => [b.bookUid, b]));
  return myEntries.map((e) => {
    const meta = map.get(e.bookUid);
    if (meta) return meta;
    return {
      bookUid: e.bookUid,
      title: e.bookUid,
      bookSpecUid: "",
      status: "",
      pdfStatus: "",
      pdfRequestedAt: null,
      createdAt: e.createdAt,
      externalRef: "",
    };
  });
}

export async function fetchBooksList(params?: BooksListQuery): Promise<Response> {
  const sp = new URLSearchParams();
  if (params?.limit != null) sp.set("limit", String(params.limit));
  if (params?.offset != null) sp.set("offset", String(params.offset));
  if (params?.pdfStatusIn != null && params.pdfStatusIn !== "") {
    sp.set("pdfStatusIn", params.pdfStatusIn);
  }
  if (params?.createdFrom != null && params.createdFrom !== "") {
    sp.set("createdFrom", params.createdFrom);
  }
  if (params?.createdTo != null && params.createdTo !== "") {
    sp.set("createdTo", params.createdTo);
  }
  const q = sp.toString();
  return fetch(`${sweetbookBooksUrl}${q ? `?${q}` : ""}`, {
    method: "GET",
    credentials: "include",
  });
}

export type BookCoverRow = {
  key: string;
  bookUid: string | null;
  imageUrl: string | null;
};

function isNonEmptyString(v: unknown): v is string {
  return typeof v === "string" && v.trim().length > 0;
}

function extractBooksArray(root: unknown): unknown[] {
  if (Array.isArray(root)) return root;
  if (root && typeof root === "object") {
    const o = root as Record<string, unknown>;
    if (Array.isArray(o.books)) return o.books;
    if (Array.isArray(o.items)) return o.items;
    if (Array.isArray(o.data)) return o.data;
    const data = o.data;
    if (Array.isArray(data)) return data;
    if (data && typeof data === "object") {
      const d = data as Record<string, unknown>;
      if (Array.isArray(d.books)) return d.books;
      if (Array.isArray(d.items)) return d.items;
      if (Array.isArray(d.list)) return d.list;
      if (Array.isArray(d.content)) return d.content;
    }
  }
  return [];
}

function pickBookUid(book: Record<string, unknown>): string | null {
  for (const k of ["bookUid", "uid", "bookUId"]) {
    const v = book[k];
    if (isNonEmptyString(v)) return v.trim();
  }
  if (typeof book.id === "string" && isNonEmptyString(book.id)) return book.id.trim();
  if (typeof book.id === "number") return String(book.id);
  return null;
}

/** API가 주는 표지·썸네일 URL (필드명이 문서마다 달라서 후보를 순서대로 탐색) */
function pickRemoteCoverUrl(book: Record<string, unknown>): string | null {
  const tryUrl = (v: unknown): string | null => {
    if (!isNonEmptyString(v)) return null;
    const t = v.trim();
    if (/^https?:\/\//i.test(t) || t.startsWith("//")) return t.startsWith("//") ? `https:${t}` : t;
    if (t.startsWith("/")) {
      const origin = process.env.NEXT_PUBLIC_SWEETBOOK_ORIGIN?.replace(/\/$/, "");
      if (origin) return `${origin}${t}`;
    }
    return null;
  };

  const directKeys = [
    "coverImageUrl",
    "coverUrl",
    "thumbnailUrl",
    "coverThumbnailUrl",
    "imageUrl",
    "coverImage",
    "thumbnail",
    "frontCoverUrl",
    "bookCoverUrl",
    "coverPhotoUrl",
    "previewUrl",
  ];
  for (const k of directKeys) {
    const u = tryUrl(book[k]);
    if (u) return u;
  }
  const cover = book.cover;
  if (cover && typeof cover === "object" && !Array.isArray(cover)) {
    const c = cover as Record<string, unknown>;
    for (const k of ["url", "imageUrl", "thumbnailUrl", "src"]) {
      const u = tryUrl(c[k]);
      if (u) return u;
    }
  }
  return null;
}

function localCoverUrlForBook(
  bookUid: string | null,
  localPhotos: LocalPhotoItem[]
): string | null {
  if (!bookUid) return null;
  const match = localPhotos
    .filter((p) => p.bookUid === bookUid)
    .sort((a, b) => a.id - b.id);
  const first = match[0];
  return first ? localPhotoAbsoluteUrl(first.fileUrl) : null;
}

/**
 * Sweetbook 책 목록(JSON)을 표지 타일용으로 정규화합니다.
 * 원격 표지 URL이 없으면 같은 bookUid의 로컬 업로드 사진(가장 오래된 id)을 표지로 씁니다.
 */
export function buildBookCoverRows(
  raw: unknown,
  localPhotos: LocalPhotoItem[]
): BookCoverRow[] {
  const arr = extractBooksArray(raw);
  return arr.map((item, index) => {
    const book =
      item && typeof item === "object" && !Array.isArray(item)
        ? (item as Record<string, unknown>)
        : {};
    const bookUid = pickBookUid(book);
    const key = bookUid ?? `idx-${index}`;
    const remote = pickRemoteCoverUrl(book);
    const local = localCoverUrlForBook(bookUid, localPhotos);
    return {
      key,
      bookUid,
      imageUrl: remote ?? local ?? null,
    };
  });
}

/**
 * DB에 저장된 북 표지(로컬 photo 파일)가 있으면 해당 URL로 타일 이미지를 덮어씁니다.
 */
export function applySavedBookCoverUrls(
  rows: BookCoverRow[],
  savedCovers: { bookUid: string; fileUrl: string }[]
): BookCoverRow[] {
  const map = new Map(
    savedCovers.map((c) => [c.bookUid, localPhotoAbsoluteUrl(c.fileUrl)])
  );
  return rows.map((row) => {
    if (row.bookUid && map.has(row.bookUid)) {
      return { ...row, imageUrl: map.get(row.bookUid)! };
    }
    return row;
  });
}

export async function fetchBookPhotos(bookUid: string): Promise<Response> {
  return fetch(sweetbookBookPhotosUrl(bookUid), {
    method: "GET",
    credentials: "include",
  });
}

export async function fetchBookGallery(bookUid: string): Promise<Response> {
  return fetch(sweetbookBookGalleryUrl(bookUid), {
    method: "GET",
    credentials: "include",
  });
}

/** 단일 이미지 — multipart 필드명 `file` (Sweetbook `/v1/books/{bookUid}/photos` 와 동일) */
export async function uploadPhoto(
  bookUid: string,
  file: File
): Promise<AxiosResponse<unknown>> {
  const formData = new FormData();
  formData.append("file", file);
  return axios.post<unknown>(sweetbookPhotoUploadUrl(bookUid), formData, {
    withCredentials: true,
  });
}
