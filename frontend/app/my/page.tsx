"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearLoggedIn } from "@/lib/auth-storage";
import { fetchMyMemberInfo } from "@/lib/member-api";
import {
  type BookPhotosEnvelope,
  fetchBookPhotos,
  fetchSweetbookBooks,
} from "@/lib/sweetbook-api";

type MemberInfo = {
  username: string;
};

export default function MyPage() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [books, setBooks] = useState<unknown>(null);
  const [error, setError] = useState<string | null>(null);

  const [bookPhotosUid, setBookPhotosUid] = useState("");
  const [bookPhotos, setBookPhotos] = useState<BookPhotosEnvelope | null>(null);
  const [bookPhotosError, setBookPhotosError] = useState<string | null>(null);
  const [bookPhotosLoading, setBookPhotosLoading] = useState(false);

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

      const booksRes = await fetchSweetbookBooks();
      if (!booksRes.ok) {
        setError(`책 목록 조회 실패 (${booksRes.status})`);
        return;
      }
      const booksData = await booksRes.json();
      setBooks(booksData);
    }

    load().catch(() => setError("서버에 연결할 수 없습니다."));
  }, [router]);

  function handleLogout() {
    clearLoggedIn();
    router.refresh();
  }

  async function handleLoadBookPhotos(e: React.FormEvent) {
    e.preventDefault();
    setBookPhotosError(null);
    setBookPhotos(null);
    const uid = bookPhotosUid.trim();
    if (!uid) {
      setBookPhotosError("북 UID를 입력하세요.");
      return;
    }
    setBookPhotosLoading(true);
    try {
      const res = await fetchBookPhotos(uid);
      const text = await res.text();
      if (!res.ok) {
        setBookPhotosError(text || `사진 목록 조회 실패 (${res.status})`);
        return;
      }
      setBookPhotos(JSON.parse(text) as BookPhotosEnvelope);
    } catch {
      setBookPhotosError("사진 목록을 불러오지 못했습니다.");
    } finally {
      setBookPhotosLoading(false);
    }
  }

  return (
    <div className="min-h-screen">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-200 p-4 dark:border-zinc-800">
        <Link href="/" className="text-sm hover:underline">
          ← 홈
        </Link>

        <div className="flex gap-2">
          <Link
            href="/create-book"
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
          >
            책 생성
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
          <h2 className="mb-2 text-lg font-semibold">생성한 책</h2>
          <div className="overflow-auto rounded bg-zinc-50 p-3 text-xs text-zinc-700 dark:bg-zinc-950 dark:text-zinc-200">
            {books ? JSON.stringify(books, null, 2) : "로딩 중..."}
          </div>
        </section>

        <section className="rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-3 text-lg font-semibold">업로드한 이미지</h2>
          <p className="mb-3 text-sm text-zinc-600 dark:text-zinc-400">
            {`북 UID를 입력한 뒤 목록을 불러오면 GET /v1/books/{bookUid}/photos 응답과 같은 형태로 표시합니다.`}
          </p>
          <form onSubmit={handleLoadBookPhotos} className="mb-4 flex flex-wrap items-end gap-2">
            <label className="flex min-w-[12rem] flex-1 flex-col gap-1 text-sm">
              북 UID
              <input
                value={bookPhotosUid}
                onChange={(e) => setBookPhotosUid(e.target.value)}
                placeholder="예: bk_xxx"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <button
              type="submit"
              disabled={bookPhotosLoading}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
            >
              {bookPhotosLoading ? "불러오는 중…" : "목록 불러오기"}
            </button>
          </form>
          {bookPhotosError ? (
            <p className="mb-3 rounded-md border border-red-200 bg-red-50 p-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              {bookPhotosError}
            </p>
          ) : null}
          {bookPhotos ? (
            <div className="space-y-3 text-sm">
              <p className="text-zinc-600 dark:text-zinc-400">
                success: {String(bookPhotos.success)} · totalCount:{" "}
                {bookPhotos.data?.totalCount ?? "—"}
              </p>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[36rem] border-collapse text-left text-xs">
                  <thead>
                    <tr className="border-b border-zinc-200 dark:border-zinc-700">
                      <th className="py-2 pr-2 font-medium">fileName</th>
                      <th className="py-2 pr-2 font-medium">originalName</th>
                      <th className="py-2 pr-2 font-medium">size</th>
                      <th className="py-2 pr-2 font-medium">mimeType</th>
                      <th className="py-2 pr-2 font-medium">uploadedAt</th>
                      <th className="py-2 font-medium">hash</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(bookPhotos.data?.photos ?? []).map((p, i) => (
                      <tr
                        key={`${p.fileName}-${i}`}
                        className="border-b border-zinc-100 dark:border-zinc-800"
                      >
                        <td className="py-2 pr-2 align-top">{p.fileName}</td>
                        <td className="py-2 pr-2 align-top">{p.originalName}</td>
                        <td className="py-2 pr-2 align-top">{p.size}</td>
                        <td className="py-2 pr-2 align-top">{p.mimeType}</td>
                        <td className="py-2 pr-2 align-top">{p.uploadedAt}</td>
                        <td className="py-2 align-top font-mono text-[10px]">{p.hash}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {(!bookPhotos.data?.photos || bookPhotos.data.photos.length === 0) ? (
                <p className="text-zinc-500">등록된 사진이 없습니다.</p>
              ) : null}
            </div>
          ) : (
            <p className="text-sm text-zinc-500">
              북 UID를 입력한 뒤 목록을 불러오세요.
            </p>
          )}
        </section>
      </main>
    </div>
  );
}

