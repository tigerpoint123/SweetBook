"use client";

import { useEffect, useRef } from "react";
import type { PageFlip as PageFlipClass } from "page-flip";
import { localPhotoAbsoluteUrl, type LocalPhotoItem } from "@/lib/photo-api";

type BookPageFlipProps = {
  photos: LocalPhotoItem[];
  /** 넓은 레이아웃(예: 갤러리 8:2)에서 뷰어를 조금 더 크게 */
  expanded?: boolean;
};

/**
 * StPageFlip(`page-flip`)으로 로컬 이미지 URL을 책 넘김 형태로 표시합니다.
 */
export function BookPageFlip({ photos, expanded = false }: BookPageFlipProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<PageFlipClass | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el || photos.length === 0) return;

    let cancelled = false;

    async function mount() {
      const { PageFlip } = await import("page-flip");
      const root = containerRef.current;
      if (cancelled || !root) return;

      root.innerHTML = "";

      const urls = photos.map((p) => localPhotoAbsoluteUrl(p.fileUrl));

      const pageFlip = new PageFlip(root, {
        width: expanded ? 456 : 399,
        height: expanded ? 608 : 532,
        size: "stretch",
        minWidth: expanded ? 285 : 266,
        maxWidth: expanded ? 646 : 494,
        minHeight: expanded ? 380 : 354,
        maxHeight: expanded ? 862 : 658,
        showCover: true,
        maxShadowOpacity: 0.45,
        flippingTime: 900,
        mobileScrollSupport: true,
        drawShadow: true,
        usePortrait: true,
      });

      pageFlip.loadFromImages(urls);
      instanceRef.current = pageFlip;
    }

    void mount();

    return () => {
      cancelled = true;
      instanceRef.current?.destroy();
      instanceRef.current = null;
      if (el) el.innerHTML = "";
    };
  }, [photos, expanded]);

  if (photos.length === 0) return null;

  return (
    <div className="w-full">
      <div
        ref={containerRef}
        className={`mx-auto w-full rounded-lg bg-zinc-200 shadow-lg dark:bg-zinc-800 ${expanded ? "max-w-[min(100%,646px)]" : "max-w-[min(100%,494px)]"}`}
        style={{ minHeight: "266px" }}
      />
      <p className="mt-3 text-center text-xs text-zinc-500 dark:text-zinc-400">
        페이지 모서리를 드래그하거나, 좌·우 가장자리를 클릭해 넘길 수 있습니다.
      </p>
    </div>
  );
}
