"use client";

import { useEffect, useRef, useState } from "react";
import type { PageFlip as PageFlipClass, PageFlipEvent } from "page-flip";
import { BookSelectedPageNav } from "@/components/BookSelectedPageNav";
import { localPhotoAbsoluteUrl, type LocalPhotoItem } from "@/lib/photo-api";

type BookPageFlipProps = {
  photos: LocalPhotoItem[];
  /** 넓은 레이아웃(예: 갤러리 8:2)에서 뷰어를 조금 더 크게 */
  expanded?: boolean;
};

/**
 * StPageFlip(`page-flip`)으로 로컬 이미지 URL을 책 넘김 형태로 표시합니다.
 * 화면이 넓으면 양쪽(한 스프레드에 두 장)으로 표시됩니다.
 * 하단 번호: 첫 장 0, 이후 짝수 장만 몫(2→1, 4→2…) 표시.
 */
export function BookPageFlip({
  photos,
  expanded = false,
}: BookPageFlipProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<PageFlipClass | null>(null);
  const [activePage1Based, setActivePage1Based] = useState(1);

  useEffect(() => {
    setActivePage1Based(1);
  }, [photos]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el || photos.length === 0) return;

    let cancelled = false;

    async function mount() {
      const { PageFlip } = await import("page-flip");
      const root = containerRef.current;
      if (cancelled || !root) return;

      root.innerHTML = "";

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

      const onFlip = (e: PageFlipEvent) => {
        if (typeof e.data === "number") {
          setActivePage1Based(e.data + 1);
        }
      };

      pageFlip.on("flip", onFlip);
      const urls = photos.map((p) => localPhotoAbsoluteUrl(p.fileUrl));
      pageFlip.loadFromImages(urls);
      instanceRef.current = pageFlip;

      requestAnimationFrame(() => {
        if (cancelled || !instanceRef.current) return;
        setActivePage1Based(instanceRef.current.getCurrentPageIndex() + 1);
      });
    }

    void mount();

    return () => {
      cancelled = true;
      const inst = instanceRef.current;
      if (inst) {
        inst.off("flip");
        inst.destroy();
      }
      instanceRef.current = null;
      if (el) el.innerHTML = "";
    };
  }, [photos, expanded]);

  function goToPage(oneBased: number) {
    const inst = instanceRef.current;
    if (!inst || oneBased < 1 || oneBased > photos.length) return;
    inst.turnToPage(oneBased - 1);
    setActivePage1Based(oneBased);
  }

  if (photos.length === 0) return null;

  const totalPages = photos.length;
  const bookMaxCls = expanded
    ? "max-w-[min(100%,646px)]"
    : "max-w-[min(100%,494px)]";

  return (
    <div className="w-full">
      <div
        ref={containerRef}
        className={`mx-auto w-full rounded-lg bg-zinc-200 shadow-lg dark:bg-zinc-800 ${bookMaxCls}`}
        style={{ minHeight: "266px" }}
      />
      <p
        className={`mx-auto mt-3 text-center text-xs text-zinc-500 dark:text-zinc-400 ${bookMaxCls}`}
      >
        페이지 모서리를 드래그하거나, 좌·우 가장자리를 클릭해 넘길 수 있습니다.
      </p>
      <BookSelectedPageNav
        totalPages={totalPages}
        activePage1Based={activePage1Based}
        onGoTo={goToPage}
        bookMaxCls={bookMaxCls}
      />
    </div>
  );
}
