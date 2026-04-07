"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { BookCoverTile } from "@/components/BookCoverTile";
import { readLoggedIn } from "@/lib/auth-storage";
import {
  buildBookCoverRows,
  fetchBooksList,
  fetchMyBookEntries,
  mergeMyBooksWithSweetbookList,
  type BooksListEnvelope,
  type MyBookEntry,
} from "@/lib/sweetbook-api";
import {
  fetchLocalPhotos,
  type LocalPhotoItem,
} from "@/lib/photo-api";

export default function MyBooksPage() {
  const router = useRouter();
  const [myEntries, setMyEntries] = useState<MyBookEntry[] | null>(null);
  const [listEnvelope, setListEnvelope] = useState<BooksListEnvelope | null>(null);
  const [localPhotos, setLocalPhotos] = useState<LocalPhotoItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [listWarning, setListWarning] = useState<string | null>(null);

  useEffect(() => {
    if (!readLoggedIn()) {
      router.replace("/login");
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      setListWarning(null);
      try {
        const myRes = await fetchMyBookEntries();
        if (cancelled) return;
        if (myRes.status === 401) {
          router.replace("/login");
          return;
        }
        if (!myRes.ok) {
          const t = await myRes.text();
          setError(t || `내 책 목록 조회 실패 (${myRes.status})`);
          setMyEntries([]);
          setListEnvelope(null);
          setLocalPhotos([]);
          return;
        }
        const entries = (await myRes.json()) as MyBookEntry[];
        if (cancelled) return;
        setMyEntries(entries);

        const [listRes, photosRes] = await Promise.all([
          fetchBooksList({ limit: 100, offset: 0 }),
          fetchLocalPhotos(),
        ]);
        if (cancelled) return;

        if (listRes.ok) {
          try {
            const text = await listRes.text();
            setListEnvelope(JSON.parse(text) as BooksListEnvelope);
          } catch {
            setListWarning("전체 책 목록 응답을 해석할 수 없어 제목만 일부 생략될 수 있습니다.");
            setListEnvelope(null);
          }
        } else {
          setListWarning(
            `Sweetbook 전체 목록을 불러오지 못했습니다 (${listRes.status}). 표지·북 UID는 그대로 표시됩니다.`
          );
          setListEnvelope(null);
        }

        const photosText = await photosRes.text();
        if (cancelled) return;
        if (photosRes.ok) {
          try {
            setLocalPhotos(JSON.parse(photosText) as LocalPhotoItem[]);
          } catch {
            setLocalPhotos([]);
          }
        } else {
          setLocalPhotos([]);
        }
      } catch {
        if (!cancelled) {
          setError("서버에 연결할 수 없습니다.");
          setMyEntries([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  const mergedBooks = useMemo(() => {
    if (myEntries === null) return [];
    return mergeMyBooksWithSweetbookList(myEntries, listEnvelope);
  }, [myEntries, listEnvelope]);

  const coverRows = useMemo(
    () =>
      myEntries === null || myEntries.length === 0
        ? []
        : buildBookCoverRows({ success: true, data: { books: mergedBooks } }, localPhotos),
    [myEntries, mergedBooks, localPhotos]
  );

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-4xl px-4 py-10">
        <h1 className="mb-1 text-2xl font-semibold">책 관리</h1>
        <p className="mb-6 text-sm text-zinc-600 dark:text-zinc-400">
          이 계정으로 이 사이트에서 생성한 책만 표시합니다. (로그인 후 책 생성 시 서버에 기록됩니다.)
        </p>

        {loading ? <p className="text-sm text-zinc-500">불러오는 중…</p> : null}
        {error ? (
          <p className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
            {error}
          </p>
        ) : null}
        {listWarning ? (
          <p className="mb-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
            {listWarning}
          </p>
        ) : null}

        {!loading && !error && myEntries !== null && myEntries.length === 0 ? (
          <p className="text-sm text-zinc-500">
            아직 생성한 책이 없습니다.{" "}
            <Link href="/create-book" className="underline">
              책 생성
            </Link>
            으로 북을 만든 뒤 다시 확인하세요.
          </p>
        ) : null}

        {!loading && !error && mergedBooks.length > 0 ? (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {mergedBooks.map((book, i) => (
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
                    {book.title?.trim() ? book.title : book.bookUid}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        ) : null}
      </main>
    </div>
  );
}
