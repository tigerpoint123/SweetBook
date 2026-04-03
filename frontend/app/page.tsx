import Link from "next/link";

export default function Home() {
  return (
    <div className="min-h-screen">
      <header className="flex justify-end gap-3 p-4">
        <Link href="/login" className="hover:underline">
          로그인
        </Link>
        <Link href="/signup" className="hover:underline">
          회원가입
        </Link>
      </header>

      <main className="flex min-h-[calc(100vh-64px)] items-center justify-center">
        <h1>Make Your Cosplayer Book</h1>
      </main>
    </div>
  );
}
