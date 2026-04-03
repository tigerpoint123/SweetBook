"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { memberApi, postMemberForm } from "@/lib/member-api";

export default function SignupPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    setPending(true);
    try {
      const res = await postMemberForm(memberApi.register, username, password);
      const text = await res.text();
      if (!res.ok) {
        setMessage(text || `요청 실패 (${res.status})`);
        return;
      }
      window.alert("회원가입이 완료되었습니다.");
      router.push("/");
    } catch {
      setMessage("서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between gap-3 border-b border-zinc-200 p-4 dark:border-zinc-800">
        <button
          type="button"
          onClick={() => router.push("/")}
          className="text-sm text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        >
          ← 홈
        </button>
        <Link href="/login" className="text-sm hover:underline">
          로그인
        </Link>
      </header>

      <main className="mx-auto flex max-w-sm flex-col gap-6 px-4 py-12">
        <h1 className="text-xl font-semibold">회원가입</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="flex flex-col gap-1 text-sm">
            아이디
            <input
              name="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            비밀번호
            <input
              name="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
            />
          </label>
          <button
            type="submit"
            disabled={pending}
            className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
          >
            {pending ? "처리 중…" : "가입하기"}
          </button>
        </form>
        {message ? (
          <p className="text-sm text-zinc-700 dark:text-zinc-300">{message}</p>
        ) : null}
      </main>
    </div>
  );
}
