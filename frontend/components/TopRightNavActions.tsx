"use client";

import Link from "next/link";
import { clearLoggedIn } from "@/lib/auth-storage";

export type NavActiveTab = "create-book" | "my" | "my-books" | null;

type Props = {
  loggedIn: boolean;
  activeTab?: NavActiveTab;
  showAuthLinksWhenLoggedOut?: boolean;
  /** 로그아웃 상태에서 해당 링크만 숨김 (로그인/회원가입 페이지 중복 방지) */
  omitAuthLink?: "login" | "signup";
  onLoggedOut?: () => void;
};

export function TopRightNavActions({
  loggedIn,
  activeTab = null,
  showAuthLinksWhenLoggedOut = true,
  omitAuthLink,
  onLoggedOut,
}: Props) {
  function renderNavItem(href: string, label: string, tab: Exclude<NavActiveTab, null>) {
    if (activeTab === tab) {
      return (
        <span
          key={tab}
          className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900"
        >
          {label}
        </span>
      );
    }
    return (
      <Link
        key={tab}
        href={href}
        className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-600 dark:hover:bg-zinc-900"
      >
        {label}
      </Link>
    );
  }

  if (!loggedIn) {
    if (!showAuthLinksWhenLoggedOut) return null;
    return (
      <>
        {omitAuthLink !== "login" ? (
          <Link href="/login" className="text-sm hover:underline">
            로그인
          </Link>
        ) : null}
        {omitAuthLink !== "signup" ? (
          <Link href="/signup" className="text-sm hover:underline">
            회원가입
          </Link>
        ) : null}
      </>
    );
  }

  return (
    <>
      {renderNavItem("/create-book", "책 생성", "create-book")}
      {renderNavItem("/my", "내 정보", "my")}
      {renderNavItem("/my-books", "책 관리", "my-books")}
      <button
        type="button"
        onClick={() => {
          clearLoggedIn();
          onLoggedOut?.();
          window.location.reload();
        }}
        className="text-sm hover:underline"
      >
        로그아웃
      </button>
    </>
  );
}
