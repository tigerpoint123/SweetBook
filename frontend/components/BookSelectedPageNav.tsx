"use client";

import {
  navButtonIsActive,
  navLabelForTarget,
  navTargetPages,
} from "@/lib/book-selected-nav";

type BookSelectedPageNavProps = {
  totalPages: number;
  activePage1Based: number;
  onGoTo: (oneBased: number) => void;
  bookMaxCls: string;
};

export function BookSelectedPageNav({
  totalPages,
  activePage1Based,
  onGoTo,
  bookMaxCls,
}: BookSelectedPageNavProps) {
  return (
    <nav
      className={`mx-auto mt-4 w-full border-t border-zinc-200 pt-4 dark:border-zinc-700 ${bookMaxCls}`}
      aria-label="책 페이지 번호"
    >
      <div className="w-full overflow-x-auto overscroll-x-contain pb-1 [-webkit-overflow-scrolling:touch]">
        <div className="flex min-w-full w-max flex-nowrap items-center justify-center gap-2">
          <span className="shrink-0 text-xs text-zinc-500 dark:text-zinc-400">페이지</span>
          <div className="flex flex-nowrap gap-1.5">
            {navTargetPages(totalPages).map((target) => {
              const label = navLabelForTarget(target);
              const isActive = navButtonIsActive(
                target,
                activePage1Based,
                totalPages
              );
              return (
                <button
                  key={target}
                  type="button"
                  aria-label={`${target}번째 이미지로 이동 (표시 ${label})`}
                  aria-current={isActive ? "page" : undefined}
                  onClick={() => onGoTo(target)}
                  className={`shrink-0 min-h-9 min-w-9 rounded-md border text-sm font-medium tabular-nums transition-colors ${
                    isActive
                      ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                      : "border-zinc-300 bg-white text-zinc-700 hover:bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}
