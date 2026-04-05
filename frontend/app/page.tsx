"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { BookCoverTile } from "@/components/BookCoverTile";
import { clearLoggedIn, readLoggedIn } from "@/lib/auth-storage";
import {
  fetchBookCovers,
  fetchLocalPhotos,
  type BookCoverItem,
  type LocalPhotoItem,
} from "@/lib/photo-api";
import {
  applySavedBookCoverUrls,
  buildBookCoverRows,
  fetchBooksList,
  type BooksListEnvelope,
} from "@/lib/sweetbook-api";

export default function HomePage() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);
  const [booksEnvelope, setBooksEnvelope] = useState<BooksListEnvelope | null>(null);
  const [bookCovers, setBookCovers] = useState<BookCoverItem[]>([]);
  const [localPhotos, setLocalPhotos] = useState<LocalPhotoItem[]>([]);
  const [booksLoading, setBooksLoading] = useState(false);
  const [booksError, setBooksError] = useState<string | null>(null);

  useEffect(() => {
    setLoggedIn(readLoggedIn());
  }, []);

  useEffect(() => {
    let cancelled = false;
    setBooksLoading(true);
    setBooksError(null);
    fetchBooksList({ limit: 48, offset: 0, finalizedOnly: true })
      .then(async (res) => {
        const text = await res.text();
        if (cancelled) return;
        if (!res.ok) {
          setBooksError(text || `책 목록 조회 실패 (${res.status})`);
          setBooksEnvelope(null);
          return;
        }
        try {
          setBooksEnvelope(JSON.parse(text) as BooksListEnvelope);
        } catch {
          setBooksError("응답을 해석할 수 없습니다.");
          setBooksEnvelope(null);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setBooksError("책 목록을 불러오지 못했습니다.");
          setBooksEnvelope(null);
        }
      })
      .finally(() => {
        if (!cancelled) setBooksLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    void fetchBookCovers().then(async (res) => {
      if (cancelled) return;
      if (!res.ok) {
        setBookCovers([]);
        return;
      }
      try {
        const data = (await res.json()) as BookCoverItem[];
        if (!cancelled) setBookCovers(Array.isArray(data) ? data : []);
      } catch {
        if (!cancelled) setBookCovers([]);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    void fetchLocalPhotos().then(async (res) => {
      if (cancelled) return;
      if (!res.ok) {
        setLocalPhotos([]);
        return;
      }
      try {
        const data = (await res.json()) as LocalPhotoItem[];
        if (!cancelled) setLocalPhotos(Array.isArray(data) ? data : []);
      } catch {
        if (!cancelled) setLocalPhotos([]);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const coverRows = useMemo(() => {
    const base = booksEnvelope ? buildBookCoverRows(booksEnvelope, localPhotos) : [];
    return applySavedBookCoverUrls(base, bookCovers);
  }, [booksEnvelope, bookCovers, localPhotos]);

  function handleLogout() {
    clearLoggedIn();
    setLoggedIn(false);
    router.refresh();
  }

  const books = booksEnvelope?.success ? (booksEnvelope.data?.books ?? []) : [];

  return (
    <div className="min-h-screen">
      <header className="flex justify-end gap-3 p-4">
        {loggedIn ? (
          <>
            <Link
              href="/create-book"
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
            >
              책 생성
            </Link>
            <Link
              href="/my"
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
            >
              내 정보
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
            <button
              type="button"
              onClick={handleLogout}
              className="text-sm hover:underline"
            >
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link href="/login" className="hover:underline">
              로그인
            </Link>
            <Link href="/signup" className="hover:underline">
              회원가입
            </Link>
          </>
        )}
      </header>

      <main className="mx-auto flex min-h-[calc(100vh-64px)] max-w-4xl flex-col gap-8 px-4 py-10">
        <div className="text-center">
          <h1 className="text-2xl font-semibold">Make Your Cosplayer Book</h1>
          {loggedIn ? (
            <p className="mt-2 text-sm text-zinc-500">
              사진을 올리려면 상단의 업로드를 누르세요.
            </p>
          ) : null}
        </div>

        <section className="w-full">
          {booksLoading ? (
            <p className="text-center text-sm text-zinc-500">불러오는 중…</p>
          ) : null}
          {booksError ? (
            <p className="text-center text-sm text-red-600 dark:text-red-400">{booksError}</p>
          ) : null}
          {!booksLoading && !booksError && booksEnvelope && !booksEnvelope.success ? (
            <p className="text-center text-sm text-zinc-500">목록을 가져오지 못했습니다.</p>
          ) : null}
          {!booksLoading && !booksError && booksEnvelope?.success && books.length === 0 ? (
            <p className="text-center text-sm text-zinc-500">
              최종화된 책이 없습니다. (메인에는 편집을 마친 책만 표시됩니다.)
            </p>
          ) : null}
          {books.length > 0 ? (
            <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
              {books.map((book, i) => (
                <li
                  key={book.bookUid}
                  className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700"
                >
                  <Link
                    href={`/book/${encodeURIComponent(book.bookUid)}`}
                    className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                  >
                    <BookCoverTile url={coverRows[i]?.imageUrl ?? null} />
                    <p className="truncate border-t border-zinc-100 bg-white px-2 py-2 text-center text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-200">
                      {book.title}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          ) : null}
        </section>
      </main>
    </div>
  );
}
