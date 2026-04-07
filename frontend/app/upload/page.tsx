"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import axios from "axios";
import { readLoggedIn } from "@/lib/auth-storage";
import { uploadPhoto } from "@/lib/sweetbook-api";

function UploadPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const bookUidFromQuery = (searchParams.get("bookUid") ?? "").trim();

  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<{ current: number; total: number } | null>(
    null
  );

  useEffect(() => {
    if (!readLoggedIn()) {
      router.replace("/login");
    }
  }, [router]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage(null);
    const fileInput = e.currentTarget.elements.namedItem("file") as HTMLInputElement;
    const fileList = fileInput?.files;
    if (!bookUidFromQuery) {
      setMessage("이 페이지는 책 상세의 「사진 업로드」로만 이용할 수 있습니다.");
      return;
    }
    if (!fileList || fileList.length === 0) {
      setMessage("이미지 파일을 하나 이상 선택하세요.");
      return;
    }
    const uid = bookUidFromQuery;
    const files = Array.from(fileList);
    setPending(true);
    setUploadProgress(null);
    type Row = { name: string; ok: boolean; detail?: string };
    const results: Row[] = [];
    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        setUploadProgress({ current: i + 1, total: files.length });
        try {
          const res = await uploadPhoto(uid, file);
          const preview =
            typeof res.data === "string"
              ? res.data
              : JSON.stringify(res.data, null, 2);
          results.push({ name: file.name, ok: true, detail: preview });
        } catch (err) {
          let detail = "알 수 없는 오류";
          if (axios.isAxiosError(err)) {
            const data = err.response?.data;
            detail =
              typeof data === "string"
                ? data
                : data != null
                  ? JSON.stringify(data)
                  : `업로드 실패 (${err.response?.status ?? "?"})`;
          } else {
            detail = "서버에 연결할 수 없습니다.";
          }
          results.push({ name: file.name, ok: false, detail });
        }
      }

      const failed = results.filter((r) => !r.ok);
      const succeeded = results.filter((r) => r.ok);

      if (failed.length === 0) {
        if (files.length === 1 && succeeded[0]?.detail) {
          setMessage(
            `업로드가 완료되었습니다.\n${succeeded[0].detail}\n\n잠시 후 책 사진 목록으로 이동합니다.`
          );
        } else {
          setMessage(
            `업로드가 완료되었습니다. (${files.length}장)\n${files.map((f) => f.name).join("\n")}\n\n잠시 후 책 사진 목록으로 이동합니다.`
          );
        }
        window.setTimeout(() => {
          router.push(`/book/${encodeURIComponent(uid)}`);
        }, 2000);
      } else if (succeeded.length === 0) {
        setMessage(
          `업로드에 모두 실패했습니다.\n${failed.map((f) => `${f.name}: ${f.detail}`).join("\n")}`
        );
      } else {
        setMessage(
          `일부만 성공했습니다. (${succeeded.length}/${files.length}장)\n\n성공:\n${succeeded.map((r) => r.name).join("\n")}\n\n실패:\n${failed.map((f) => `${f.name}: ${f.detail}`).join("\n")}`
        );
      }
    } finally {
      setPending(false);
      setUploadProgress(null);
    }
  }

  const missingBookUid = bookUidFromQuery === "";

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-md px-4 py-10">
        <div className="mb-4 text-sm">
          {bookUidFromQuery ? (
            <Link
              href={`/book/${encodeURIComponent(bookUidFromQuery)}`}
              className="hover:underline"
            >
              ← 책 사진 목록
            </Link>
          ) : (
            <Link href="/my-books" className="hover:underline">
              ← 책 관리
            </Link>
          )}
        </div>
        <h1 className="mb-6 text-xl font-semibold">사진 업로드</h1>
        {missingBookUid ? (
          <p className="mb-6 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-100">
            업로드할 책이 지정되지 않았습니다.{" "}
            <Link href="/my-books" className="font-medium underline">
              책 관리
            </Link>
            에서 책을 연 뒤 「사진 업로드」를 이용하세요.
          </p>
        ) : (
          <p className="mb-4 font-mono text-xs text-zinc-500 dark:text-zinc-400">{bookUidFromQuery}</p>
        )}
        <aside
          role="note"
          className="mb-6 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-100"
        >
          <p className="font-medium">책 보기 화면 권장 규격</p>
          <p className="mt-1 text-amber-900/90 dark:text-amber-200/90">
            세로형(포트레이트)으로 가로·세로 비율 약{" "}
            <strong className="font-semibold">3 : 4</strong>를 맞추는 것을 권장합니다.
          </p>
        </aside>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="flex flex-col gap-1 text-sm">
            이미지 (여러 장 선택 가능, 순서대로 한 장씩 업로드)
            <input
              name="file"
              type="file"
              accept="image/*"
              multiple
              required
              disabled={missingBookUid}
              className="text-sm file:mr-3 file:rounded-md file:border-0 file:bg-zinc-100 file:px-3 file:py-1.5 file:text-sm disabled:opacity-50 dark:file:bg-zinc-800"
            />
          </label>
          <button
            type="submit"
            disabled={pending || missingBookUid}
            className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
          >
            {pending && uploadProgress
              ? `업로드 중… (${uploadProgress.current}/${uploadProgress.total})`
              : pending
                ? "업로드 중…"
                : "업로드"}
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

export default function UploadPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center text-sm text-zinc-500">
          불러오는 중…
        </div>
      }
    >
      <UploadPageContent />
    </Suspense>
  );
}
