"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearLoggedIn } from "@/lib/auth-storage";
import { fetchMyMemberInfo } from "@/lib/member-api";
import { fetchSweetbookBooks } from "@/lib/sweetbook-api";

type MemberInfo = {
  username: string;
};

export default function MyPage() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [books, setBooks] = useState<unknown>(null);
  const [error, setError] = useState<string | null>(null);

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

        <section className="rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-2 text-lg font-semibold">생성한 책</h2>
          <div className="overflow-auto rounded bg-zinc-50 p-3 text-xs text-zinc-700 dark:bg-zinc-950 dark:text-zinc-200">
            {books ? JSON.stringify(books, null, 2) : "로딩 중..."}
          </div>
        </section>
      </main>
    </div>
  );
}

