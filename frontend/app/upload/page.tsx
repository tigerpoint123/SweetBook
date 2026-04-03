"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import axios from "axios";
import { readLoggedIn } from "@/lib/auth-storage";
import { uploadPhoto } from "@/lib/sweetbook-api";

export default function UploadPage() {
  const router = useRouter();
  const [bookUid, setBookUid] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!readLoggedIn()) {
      router.replace("/login");
    }
  }, [router]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage(null);
    const fileInput = e.currentTarget.elements.namedItem("file") as HTMLInputElement;
    const file = fileInput?.files?.[0];
    if (!bookUid.trim()) {
      setMessage("북 UID를 입력하세요.");
      return;
    }
    if (!file) {
      setMessage("이미지 파일을 한 장 선택하세요.");
      return;
    }
    setPending(true);
    try {
      const res = await uploadPhoto(bookUid.trim(), file);
      const preview =
        typeof res.data === "string"
          ? res.data
          : JSON.stringify(res.data, null, 2);
      setMessage(
        preview ? `업로드가 완료되었습니다.\n${preview}` : "업로드가 완료되었습니다."
      );
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data;
        const body =
          typeof data === "string"
            ? data
            : data != null
              ? JSON.stringify(data)
              : "";
        setMessage(
          body || `업로드 실패 (${err.response?.status ?? "?"})`
        );
      } else {
        setMessage("서버에 연결할 수 없습니다.");
      }
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
        <div className="flex gap-2">
          <Link
            href="/create-book"
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium dark:border-zinc-600"
          >
            책 생성
          </Link>
          <span className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900">
            업로드
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-md px-4 py-10">
        <h1 className="mb-6 text-xl font-semibold">사진 업로드</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="flex flex-col gap-1 text-sm">
            북 UID
            <input
              name="bookUid"
              value={bookUid}
              onChange={(e) => setBookUid(e.target.value)}
              required
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            이미지 (한 장)
            <input
              name="file"
              type="file"
              accept="image/*"
              required
              className="text-sm file:mr-3 file:rounded-md file:border-0 file:bg-zinc-100 file:px-3 file:py-1.5 file:text-sm dark:file:bg-zinc-800"
            />
          </label>
          <button
            type="submit"
            disabled={pending}
            className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
          >
            {pending ? "업로드 중…" : "업로드"}
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
