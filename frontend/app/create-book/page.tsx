"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import { createBook } from "@/lib/sweetbook-api";

export default function CreateBookPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [bookSpecUid, setBookSpecUid] = useState("SQUAREBOOK_HC");
  const [bookAuthor, setBookAuthor] = useState("");
  const [externalRef, setExternalRef] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!readLoggedIn()) {
      router.replace("/login");
    }
  }, [router]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    if (!title.trim()) {
      setMessage("책 제목을 입력하세요.");
      return;
    }
    if (!bookSpecUid.trim()) {
      setMessage("책 규격 UID를 입력하세요.");
      return;
    }
    if (!bookAuthor.trim()) {
      setMessage("저자를 입력하세요.");
      return;
    }
    if (!externalRef.trim()) {
      setMessage("외부 참조 ID를 입력하세요.");
      return;
    }
    setPending(true);
    try {
      const res = await createBook({
        title: title.trim(),
        bookSpecUid: bookSpecUid.trim(),
        bookAuthor: bookAuthor.trim(),
        externalRef: externalRef.trim(),
      });
      const text = await res.text();
      if (!res.ok) {
        setMessage(text || `생성 실패 (${res.status})`);
        return;
      }
      setMessage(
        "책이 성공적으로 생성되었습니다. 잠시 후 메인 페이지로 이동합니다."
      );
      window.setTimeout(() => {
        router.replace("/");
      }, 2000);
    } catch {
      setMessage("서버에 연결할 수 없습니다.");
    } finally {
      setPending(false);
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
            href="/my-books"
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
          >
            책 관리
          </Link>
          <Link
            href="/upload"
            className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900"
          >
            업로드
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-md px-4 py-10">
        <h1 className="mb-6 text-xl font-semibold">책 생성</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="flex flex-col gap-1 text-sm">
            책 제목
            <input
              name="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            책 규격 UID (bookSpecUid)
            <input
              name="bookSpecUid"
              value={bookSpecUid}
              onChange={(e) => setBookSpecUid(e.target.value)}
              required
              placeholder="예: SQUAREBOOK_HC"
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            저자 (bookAuthor)
            <input
              name="bookAuthor"
              value={bookAuthor}
              onChange={(e) => setBookAuthor(e.target.value)}
              required
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            외부 참조 ID (externalRef)
            <input
              name="externalRef"
              value={externalRef}
              onChange={(e) => setExternalRef(e.target.value)}
              required
              placeholder="예: partner-book-001"
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <button
            type="submit"
            disabled={pending}
            className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
          >
            {pending ? "생성 중…" : "책 만들기"}
          </button>
        </form>
        {message ? (
          <p className="mt-4 whitespace-pre-wrap break-words text-sm text-zinc-700 dark:text-zinc-300">
            {message}
          </p>
        ) : null}
      </main>
    </div>
  );
}
