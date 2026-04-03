"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearLoggedIn, readLoggedIn } from "@/lib/auth-storage";

export default function HomePage() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(readLoggedIn());
  }, []);

  function handleLogout() {
    clearLoggedIn();
    setLoggedIn(false);
    router.refresh();
  }

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

      <main className="flex min-h-[calc(100vh-64px)] flex-col items-center justify-center gap-6 px-4">
        <h1 className="text-center text-2xl font-semibold">
          Make Your Cosplayer Book
        </h1>
        {loggedIn ? (
          <p className="text-center text-sm text-zinc-500">
            사진을 올리려면 상단의 업로드를 누르세요.
          </p>
        ) : null}
      </main>
    </div>
  );
}
