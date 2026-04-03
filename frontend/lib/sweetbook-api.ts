import { API_BASE } from "@/lib/api-config";

export const sweetbookBooksUrl = `${API_BASE}/api/sweetbook/books`;

export function sweetbookPhotoUploadUrl(bookUid: string): string {
  return `${API_BASE}/api/sweetbook/books/${encodeURIComponent(bookUid)}/photos`;
}

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

export async function uploadPhotos(
  bookUid: string,
  files: FileList | File[]
): Promise<Response> {
  const formData = new FormData();
  const list = Array.from(files);
  for (const file of list) {
    formData.append("files", file);
  }
  return fetch(sweetbookPhotoUploadUrl(bookUid), {
    method: "POST",
    credentials: "include",
    body: formData,
  });
}
