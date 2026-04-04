"use client";

import { useState } from "react";

export function BookCoverTile({ url }: { url: string | null }) {
  const [broken, setBroken] = useState(false);

  if (!url || broken) {
    return (
      <div className="flex aspect-square w-full items-center justify-center bg-zinc-100 px-2 text-center text-sm text-zinc-500 dark:bg-zinc-900 dark:text-zinc-400">
        표지가 없습니다
      </div>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt=""
      className="aspect-square w-full object-cover bg-zinc-100 dark:bg-zinc-900"
      onError={() => setBroken(true)}
    />
  );
}
