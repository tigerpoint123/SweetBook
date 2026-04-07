"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import { TopRightNavActions, type NavActiveTab } from "@/components/TopRightNavActions";

function activeTabFromPath(pathname: string): NavActiveTab {
  if (pathname === "/create-book" || pathname.startsWith("/create-book/")) {
    return "create-book";
  }
  if (pathname === "/my-books" || pathname.startsWith("/my-books/")) {
    return "my-books";
  }
  if (pathname === "/my" || pathname.startsWith("/my/")) {
    return "my";
  }
  return null;
}

function omitAuthLinkFromPath(pathname: string): "login" | "signup" | undefined {
  if (pathname === "/login" || pathname.startsWith("/login/")) return "login";
  if (pathname === "/signup" || pathname.startsWith("/signup/")) return "signup";
  return undefined;
}

export function SiteHeader() {
  const pathname = usePathname() ?? "";
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    queueMicrotask(() => {
      setLoggedIn(readLoggedIn());
    });
  }, [pathname]);

  return (
    <header className="sticky top-0 z-40 flex flex-wrap items-center justify-between gap-3 border-b border-zinc-200 bg-white/95 px-4 py-3 backdrop-blur dark:border-zinc-800 dark:bg-zinc-950/95">
      <Link
        href="/"
        className="text-sm font-medium text-zinc-800 hover:underline dark:text-zinc-200"
      >
        홈
      </Link>
      <div className="flex flex-wrap items-center gap-2">
        <TopRightNavActions
          loggedIn={loggedIn}
          activeTab={activeTabFromPath(pathname)}
          omitAuthLink={omitAuthLinkFromPath(pathname)}
        />
      </div>
    </header>
  );
}
