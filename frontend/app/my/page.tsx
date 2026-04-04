"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { BookCoverTile } from "@/components/BookCoverTile";
import { clearLoggedIn } from "@/lib/auth-storage";
import { fetchMyMemberInfo } from "@/lib/member-api";
import {
  downloadLocalPhoto,
  fetchLocalPhotos,
  localPhotoAbsoluteUrl,
  type LocalPhotoItem,
} from "@/lib/photo-api";
import {
  buildBookCoverRows,
  deleteSweetbookBook,
  fetchBooksList,
  fetchMyBookEntries,
  mergeMyBooksWithSweetbookList,
  type BooksListEnvelope,
  type DeleteBookResponse,
  type MyBookEntry,
} from "@/lib/sweetbook-api";

type MemberInfo = {
  username: string;
};

export default function MyPage() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [books, setBooks] = useState<unknown>(null);
  const [error, setError] = useState<string | null>(null);

  const [localPhotosFilterUid, setLocalPhotosFilterUid] = useState("");
  const [localPhotos, setLocalPhotos] = useState<LocalPhotoItem[] | null>(null);
  const [localPhotosError, setLocalPhotosError] = useState<string | null>(null);
  const [localPhotosLoading, setLocalPhotosLoading] = useState(false);

  const [bookDeleteMode, setBookDeleteMode] = useState(false);
  const [selectedBookUids, setSelectedBookUids] = useState<Set<string>>(() => new Set());
  const [bookDeletePending, setBookDeletePending] = useState(false);
  const [bookDeleteMessage, setBookDeleteMessage] = useState<string | null>(null);

  const bookCoverRows = useMemo(
    () => (books == null ? null : buildBookCoverRows(books, localPhotos ?? [])),
    [books, localPhotos]
  );

  const reloadMergedBooks = useCallback(async () => {
    const myRes = await fetchMyBookEntries();
    if (myRes.status === 401) {
      router.replace("/login");
      return;
    }
    if (!myRes.ok) {
      const t = await myRes.text();
      setError(t || `내 책 목록 조회 실패 (${myRes.status})`);
      setBooks({ success: true, data: { books: [] } });
      return;
    }
    const myEntries = (await myRes.json()) as MyBookEntry[];

    const booksRes = await fetchBooksList({ limit: 100, offset: 0 });
    let envelope: BooksListEnvelope | null = null;
    if (booksRes.ok) {
      try {
        envelope = (await booksRes.json()) as BooksListEnvelope;
      } catch {
        setError("책 목록 응답을 해석할 수 없습니다.");
      }
    } else {
      setError(`Sweetbook 책 목록 조회 실패 (${booksRes.status})`);
    }
    const merged = mergeMyBooksWithSweetbookList(myEntries, envelope);
    setBooks({ success: true, data: { books: merged } });
  }, [router]);

  useEffect(() => {
    async function load() {
      setError(null);

      const meRes = await fetchMyMemberInfo();
      if (meRes.status === 401) {
        router.replace("/login");
        return;
      }
      if (!meRes.ok) {
        setError(`내 정보 조회 실패 (${meRes.status})`);
        return;
      }

      const me = (await meRes.json()) as MemberInfo;
      setUsername(me.username);

      setLocalPhotosError(null);
      setLocalPhotosLoading(true);
      try {
        const photosRes = await fetchLocalPhotos();
        const photosText = await photosRes.text();
        if (!photosRes.ok) {
          setLocalPhotosError(photosText || `로컬 사진 목록 실패 (${photosRes.status})`);
          setLocalPhotos([]);
        } else {
          setLocalPhotos(JSON.parse(photosText) as LocalPhotoItem[]);
        }
      } catch {
        setLocalPhotosError("로컬 사진 목록을 불러오지 못했습니다.");
        setLocalPhotos([]);
      } finally {
        setLocalPhotosLoading(false);
      }

      await reloadMergedBooks();
    }

    void load().catch(() => setError("서버에 연결할 수 없습니다."));
  }, [router, reloadMergedBooks]);

  function handleLogout() {
    clearLoggedIn();
    router.refresh();
  }

  function toggleBookDeleteMode() {
    setBookDeleteMessage(null);
    setBookDeleteMode((m) => {
      const next = !m;
      if (!next) setSelectedBookUids(new Set());
      return next;
    });
  }

  function toggleBookUidSelected(uid: string) {
    setSelectedBookUids((prev) => {
      const next = new Set(prev);
      if (next.has(uid)) next.delete(uid);
      else next.add(uid);
      return next;
    });
  }

  async function handleBookDeleteComplete() {
    setBookDeleteMessage(null);
    if (selectedBookUids.size === 0) {
      setBookDeleteMessage("삭제할 책을 선택하세요.");
      return;
    }
    setBookDeletePending(true);
    try {
      const uids = Array.from(selectedBookUids);
      let fail = 0;
      let lastMsg = "";
      for (const uid of uids) {
        const res = await deleteSweetbookBook(uid);
        const text = await res.text();
        if (!res.ok) {
          fail++;
          try {
            const j = JSON.parse(text) as { message?: string };
            if (j.message) lastMsg = j.message;
          } catch {
            /* keep lastMsg */
          }
          continue;
        }
        try {
          const j = JSON.parse(text) as DeleteBookResponse;
          if (j.message) lastMsg = j.message;
        } catch {
          /* keep */
        }
      }
      await reloadMergedBooks();
      setBookDeleteMode(false);
      setSelectedBookUids(new Set());
      if (fail > 0) {
        setBookDeleteMessage(
          fail === uids.length
            ? lastMsg || `${fail}권 삭제에 실패했습니다.`
            : `${uids.length - fail}권 삭제됨. ${fail}권 실패${lastMsg ? `: ${lastMsg}` : ""}`
        );
      } else {
        setBookDeleteMessage(lastMsg || "선택한 책을 삭제했습니다.");
      }
    } catch {
      setBookDeleteMessage("네트워크 오류로 삭제할 수 없습니다.");
    } finally {
      setBookDeletePending(false);
    }
  }

  async function handleLoadLocalPhotos(e: React.FormEvent) {
    e.preventDefault();
    setLocalPhotosError(null);
    setLocalPhotosLoading(true);
    try {
      const uid = localPhotosFilterUid.trim();
      const res = await fetchLocalPhotos(uid || undefined);
      const text = await res.text();
      if (!res.ok) {
        setLocalPhotosError(text || `로컬 사진 목록 실패 (${res.status})`);
        setLocalPhotos([]);
        return;
      }
      setLocalPhotos(JSON.parse(text) as LocalPhotoItem[]);
    } catch {
      setLocalPhotosError("로컬 사진 목록을 불러오지 못했습니다.");
      setLocalPhotos([]);
    } finally {
      setLocalPhotosLoading(false);
    }
  }

  return (
    <div className="min-h-screen">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-200 p-4 dark:border-zinc-800">
        <Link href="/" className="text-sm hover:underline">
          ← 홈
        </Link>

        <div className="flex flex-wrap gap-2">
          <Link
            href="/create-book"
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
          >
            책 생성
          </Link>
          <Link
            href="/my-books"
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
          >
            책 관리
          </Link>
          <Link
            href="/upload"
            className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            업로드
          </Link>
          <button type="button" onClick={handleLogout} className="text-sm hover:underline">
            로그아웃
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-10">
        <h1 className="mb-6 text-2xl font-semibold">내 정보</h1>

        {error ? (
          <p className="mb-6 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
            {error}
          </p>
        ) : null}

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-2 text-lg font-semibold">사용자</h2>
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            username: {username ?? "로딩 중..."}
          </p>
        </section>

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold">생성한 책</h2>
            {bookCoverRows && bookCoverRows.length > 0 ? (
              <>
                <button
                  type="button"
                  onClick={toggleBookDeleteMode}
                  className={`rounded-md border px-2.5 py-1 text-xs font-medium dark:border-zinc-600 ${
                    bookDeleteMode
                      ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                      : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50 dark:bg-zinc-950 dark:text-zinc-200 dark:hover:bg-zinc-900"
                  }`}
                >
                  {bookDeleteMode ? "삭제 취소" : "책 삭제"}
                </button>
                {bookDeleteMode ? (
                  <button
                    type="button"
                    disabled={bookDeletePending}
                    onClick={() => void handleBookDeleteComplete()}
                    className="rounded-md bg-red-700 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-800 disabled:opacity-50 dark:bg-red-800 dark:hover:bg-red-700"
                  >
                    {bookDeletePending ? "삭제 중…" : "선택 완료"}
                  </button>
                ) : null}
              </>
            ) : null}
          </div>
          {bookDeleteMessage ? (
            <p className="mb-3 text-xs text-zinc-600 dark:text-zinc-400">{bookDeleteMessage}</p>
          ) : null}
          {bookCoverRows === null ? (
            <p className="text-sm text-zinc-500">로딩 중…</p>
          ) : bookCoverRows.length === 0 ? (
            <p className="text-sm text-zinc-500">생성한 책이 없습니다.</p>
          ) : (
            <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
              {bookCoverRows.map((row) => (
                <li
                  key={row.key}
                  className="relative overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700"
                >
                  {bookDeleteMode && row.bookUid ? (
                    <div className="pointer-events-auto absolute left-1 top-1 z-10 rounded bg-white/90 p-0.5 shadow dark:bg-zinc-900/90">
                      <input
                        type="checkbox"
                        checked={selectedBookUids.has(row.bookUid)}
                        onChange={() => toggleBookUidSelected(row.bookUid!)}
                        aria-label={`삭제 선택: ${row.bookUid}`}
                        className="h-4 w-4 rounded border-zinc-300 text-zinc-900 focus:ring-2 focus:ring-zinc-400 dark:border-zinc-600 dark:bg-zinc-950 dark:focus:ring-zinc-500"
                      />
                    </div>
                  ) : null}
                  {bookDeleteMode && row.bookUid ? (
                    <button
                      type="button"
                      title="클릭하여 삭제 선택"
                      className="block w-full cursor-pointer rounded-lg p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                      onClick={() => toggleBookUidSelected(row.bookUid)}
                    >
                      <BookCoverTile url={row.imageUrl} />
                    </button>
                  ) : row.bookUid ? (
                    <Link
                      href={`/book/${encodeURIComponent(row.bookUid)}`}
                      className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                    >
                      <BookCoverTile url={row.imageUrl} />
                    </Link>
                  ) : (
                    <BookCoverTile url={row.imageUrl} />
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-3 text-lg font-semibold">업로드한 이미지</h2>
          <p className="mb-3 text-sm text-zinc-600 dark:text-zinc-400">
            로컬에 저장된 업로드 이미지입니다. 북 UID는 선택(비우면 전체).
          </p>
          <form onSubmit={handleLoadLocalPhotos} className="mb-4 flex flex-wrap items-end gap-2">
            <label className="flex min-w-[12rem] flex-1 flex-col gap-1 text-sm">
              북 UID (선택)
              <input
                value={localPhotosFilterUid}
                onChange={(e) => setLocalPhotosFilterUid(e.target.value)}
                placeholder="비우면 전체 · 예: bk_xxx"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <button
              type="submit"
              disabled={localPhotosLoading}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
            >
              {localPhotosLoading ? "불러오는 중…" : "다시 불러오기"}
            </button>
          </form>
          {localPhotosError ? (
            <p className="mb-3 rounded-md border border-red-200 bg-red-50 p-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              {localPhotosError}
            </p>
          ) : null}
          {localPhotos === null ? (
            <p className="text-sm text-zinc-500">로딩 중…</p>
          ) : localPhotos.length === 0 ? (
            <p className="text-sm text-zinc-500">등록된 로컬 사진이 없습니다.</p>
          ) : (
            <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
              {localPhotos.map((p) => (
                <li
                  key={p.id}
                  className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700"
                >
                  <button
                    type="button"
                    title="클릭하여 이 기기에 저장"
                    className="block w-full cursor-pointer rounded-lg p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-zinc-950"
                    onClick={() =>
                      void downloadLocalPhoto(
                        p.fileUrl,
                        p.originalName || `photo-${p.id}`
                      ).catch(() => {
                        alert("이미지를 저장할 수 없습니다.");
                      })
                    }
                  >
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={localPhotoAbsoluteUrl(p.fileUrl)}
                      alt={p.originalName}
                      className="aspect-square w-full object-cover bg-zinc-100 dark:bg-zinc-900 pointer-events-none"
                    />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}

