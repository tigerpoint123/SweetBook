import axios, { type AxiosResponse } from "axios";
import { API_BASE } from "@/lib/api-config";

export const sweetbookBooksUrl = `${API_BASE}/api/sweetbook/books`;

export function sweetbookPhotoUploadUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/photos`;
}

/** GET /v1/books/{bookUid}/photos 프록시 */
export function sweetbookBookPhotosUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/photos`;
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

export async function createBook(payload: CreateBookPayload): Promise<Response> {
  return fetch(sweetbookBooksUrl, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function fetchSweetbookBooks(): Promise<Response> {
  return fetch(sweetbookBooksUrl, {
    method: "GET",
    credentials: "include",
  });
}

export async function fetchBookPhotos(bookUid: string): Promise<Response> {
  return fetch(sweetbookBookPhotosUrl(bookUid), {
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
