"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import {
  downloadLocalPhoto,
  fetchLocalPhotos,
  fetchSelectedPhotosForBook,
  localPhotoAbsoluteUrl,
  appendBookSelection,
  saveBookCover,
  type LocalPhotoItem,
} from "@/lib/photo-api";
import { BookPageFlip } from "@/components/BookPageFlip";
import {
  deleteBookPhoto,
  fetchBookGallery,
  fetchBookPhotos,
  fetchMyBookEntries,
  postBookContents,
  postBookFinalization,
  resolveFinalizeFailureMessage,
  SWEETBOOK_DEFAULT_CONTENTS_TEMPLATE_UID,
  uploadBookCover,
  type AddBookContentsResponse,
  type BookGalleryData,
  type BookPhotosEnvelope,
  type FinalizeBookResponse,
  type MyBookEntry,
  type UploadBookCoverResponse,
} from "@/lib/sweetbook-api";

type ApiErrorBody = {
  message?: string;
  errors?: string[];
};

function resolvePhotoSelectFailureMessage(rawText: string, status: number): string {
  let detail = rawText || `요청 실패 (${status})`;
  try {
    const j = JSON.parse(rawText) as ApiErrorBody;
    const firstError = Array.isArray(j.errors)
      ? j.errors.find((v) => typeof v === "string" && v.trim())
      : null;
    if (firstError?.includes("표지를 먼저 추가")) {
      return "표지 선택 먼저 해주세요.";
    }
    if (j?.message) detail = j.message;
    if (firstError && !j?.message) detail = firstError;
  } catch {
    /* keep detail */
  }
  return detail;
}

export default function BookGalleryPage() {
  const params = useParams();
  const bookUid = typeof params.bookUid === "string" ? params.bookUid : "";

  const [loggedIn, setLoggedIn] = useState(false);
  const [localPhotos, setLocalPhotos] = useState<LocalPhotoItem[]>([]);
  /** 책 넘김(왼쪽): selected_photo.id 오름차순 — 가장 최근 채택이 맨 뒤 페이지 */
  const [selectedFlipPhotos, setSelectedFlipPhotos] = useState<LocalPhotoItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sweetbookError, setSweetbookError] = useState<string | null>(null);
  const [localLoadError, setLocalLoadError] = useState<string | null>(null);
  const [photoSelectMode, setPhotoSelectMode] = useState(false);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<number[]>([]);
  const [photoCompletePending, setPhotoCompletePending] = useState(false);
  const [photoSelectMessage, setPhotoSelectMessage] = useState<string | null>(null);
  const [photoDeleteMode, setPhotoDeleteMode] = useState(false);
  const [selectedDeletePhotoIds, setSelectedDeletePhotoIds] = useState<number[]>([]);
  const [photoDeletePending, setPhotoDeletePending] = useState(false);
  const [photoDeleteMessage, setPhotoDeleteMessage] = useState<string | null>(null);
  const [coverSelectMode, setCoverSelectMode] = useState(false);
  const [selectedCoverPhotoId, setSelectedCoverPhotoId] = useState<number | null>(null);
  const [coverSavePending, setCoverSavePending] = useState(false);
  const [coverMessage, setCoverMessage] = useState<string | null>(null);
  /** 로그인 사용자가 이 북 생성자인지(백엔드 sweetbook_book) */
  const [isOwnerOfThisBook, setIsOwnerOfThisBook] = useState(false);
  /** 최종화되어 편집 UI(채택·삭제·표지) 비활성 */
  const [bookFinalized, setBookFinalized] = useState(false);
  const [finalizedBannerMessage, setFinalizedBannerMessage] = useState<string | null>(null);
  /** 최종화 실패(예: 최소 페이지 미달) 시 경고 스타일 */
  const [finalizeBannerIsError, setFinalizeBannerIsError] = useState(false);
  const [finalizePending, setFinalizePending] = useState(false);
  /** 갤러리 API 기준 최종화 여부(비소유자·비로그인 포함) */
  const [galleryFinalized, setGalleryFinalized] = useState(false);

  async function loadUploadedPhotosViaSweetbook(
    uid: string
  ): Promise<{ photos: LocalPhotoItem[]; errorMessage: string | null }> {
    const [swRes, localRes] = await Promise.all([
      fetchBookPhotos(uid),
      fetchLocalPhotos(uid),
    ]);
    const swText = await swRes.text();
    const localText = await localRes.text();

    if (!swRes.ok) {
      return {
        photos: [],
        errorMessage: swText || `스윗북 사진 목록 조회 실패 (${swRes.status})`,
      };
    }

    let swRows:
      | {
          fileName: string;
          originalName: string;
          size: number;
          mimeType: string;
          uploadedAt: string;
          hash: string;
        }[]
      | null = null;
    try {
      const env = JSON.parse(swText) as BookPhotosEnvelope;
      swRows = Array.isArray(env?.data?.photos) ? env.data.photos : [];
    } catch {
      return { photos: [], errorMessage: "스윗북 사진 목록 응답을 해석할 수 없습니다." };
    }

    let localRows: LocalPhotoItem[] = [];
    if (localRes.ok) {
      try {
        const parsed = JSON.parse(localText) as LocalPhotoItem[];
        localRows = Array.isArray(parsed) ? parsed : [];
      } catch {
        // keep []
      }
    }

    const bySweetbookFileName = new Map(
      localRows
        .filter((p) => typeof p.sweetbookFileName === "string" && p.sweetbookFileName.trim() !== "")
        .map((p) => [p.sweetbookFileName.trim(), p] as const)
    );

    const photos = swRows
      .map((r, idx) => {
        const local = bySweetbookFileName.get(r.fileName.trim());
        if (!local) return null;
        return {
          ...local,
          id: local.id ?? idx + 1,
          bookUid: uid,
          sweetbookFileName: r.fileName,
          originalName: r.originalName ?? local.originalName,
          size: Number.isFinite(r.size) ? r.size : local.size,
          mimeType: r.mimeType ?? local.mimeType,
          uploadedAt: r.uploadedAt ?? local.uploadedAt,
          hash: r.hash ?? local.hash,
        } satisfies LocalPhotoItem;
      })
      .filter((p): p is LocalPhotoItem => p !== null);

    const missingCount = swRows.length - photos.length;
    if (missingCount > 0) {
      return {
        photos,
        errorMessage: `스윗북 사진 ${missingCount}장은 로컬 메타데이터가 없어 목록에서 제외되었습니다.`,
      };
    }
    return { photos, errorMessage: null };
  }

  useEffect(() => {
    setLoggedIn(readLoggedIn());
  }, []);

  useEffect(() => {
    if (!loggedIn || !bookUid.trim()) {
      setIsOwnerOfThisBook(false);
      setBookFinalized(false);
      setFinalizedBannerMessage(null);
      setFinalizeBannerIsError(false);
      return;
    }
    let cancelled = false;
    void fetchMyBookEntries().then(async (res) => {
      if (cancelled) return;
      if (res.status === 401 || !res.ok) {
        setIsOwnerOfThisBook(false);
        setBookFinalized(false);
        setFinalizedBannerMessage(null);
        setFinalizeBannerIsError(false);
        return;
      }
      try {
        const entries = (await res.json()) as MyBookEntry[];
        const uid = bookUid.trim();
        const entry =
          Array.isArray(entries) ? entries.find((e) => e.bookUid === uid) : undefined;
        const owns = !!entry;
        if (cancelled) return;
        setIsOwnerOfThisBook(owns);
        const fin = entry?.finalized === true;
        setBookFinalized(fin);
        if (owns && fin) {
          setFinalizedBannerMessage("이미 최종화된 책입니다");
          setFinalizeBannerIsError(false);
        } else {
          setFinalizedBannerMessage(null);
          setFinalizeBannerIsError(false);
        }
      } catch {
        if (!cancelled) {
          setIsOwnerOfThisBook(false);
          setBookFinalized(false);
          setFinalizedBannerMessage(null);
          setFinalizeBannerIsError(false);
        }
      }
    });
    return () => {
      cancelled = true;
    };
  }, [bookUid, loggedIn]);

  useEffect(() => {
    if (!isOwnerOfThisBook || bookFinalized) {
      setPhotoSelectMode(false);
      setSelectedPhotoIds([]);
      setPhotoSelectMessage(null);
      setPhotoDeleteMode(false);
      setSelectedDeletePhotoIds([]);
      setPhotoDeleteMessage(null);
      setCoverSelectMode(false);
      setSelectedCoverPhotoId(null);
      setCoverMessage(null);
    }
  }, [isOwnerOfThisBook, bookFinalized]);

  useEffect(() => {
    if (!bookUid) {
      setLoading(false);
      setError("북 UID가 없습니다.");
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setSweetbookError(null);
    setLocalLoadError(null);
    setLocalPhotos([]);
    setSelectedFlipPhotos([]);
    setGalleryFinalized(false);

    async function load() {
      try {
        const [gRes, swPhotos, sRes] = await Promise.all([
          fetchBookGallery(bookUid),
          loadUploadedPhotosViaSweetbook(bookUid),
          fetchSelectedPhotosForBook(bookUid),
        ]);
        if (cancelled) return;

        const locals = swPhotos.photos;
        setLocalLoadError(swPhotos.errorMessage);
        setLocalPhotos(locals);

        const sText = await sRes.text();
        let selected: LocalPhotoItem[] = [];
        if (sRes.ok) {
          try {
            selected = JSON.parse(sText) as LocalPhotoItem[];
            if (!Array.isArray(selected)) selected = [];
          } catch {
            /* keep [] */
          }
        }
        if (!cancelled) setSelectedFlipPhotos(selected);

        const gText = await gRes.text();
        if (cancelled) return;

        if (gRes.status === 404) {
          if (locals.length === 0 && selected.length === 0) {
            setError(
              gText ||
                "로컬에 등록된 책이 아니거나 사진 이력이 없습니다. (책 생성 또는 업로드 후 이용하세요.)"
            );
          }
          return;
        }
        if (!gRes.ok) {
          setSweetbookError(gText || `사진 목록 확인 실패 (${gRes.status})`);
          setGalleryFinalized(false);
        } else {
          try {
            const env = JSON.parse(gText) as {
              data?: BookGalleryData;
            };
            setGalleryFinalized(env.data?.finalized === true);
          } catch {
            setGalleryFinalized(false);
          }
        }
      } catch {
        if (!cancelled) {
          setError("사진 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [bookUid]);

  const hasBookSection = localPhotos.length > 0 || selectedFlipPhotos.length > 0;

  function togglePhotoSelectMode() {
    setPhotoSelectMessage(null);
    setPhotoDeleteMessage(null);
    setCoverMessage(null);
    setCoverSelectMode(false);
    setSelectedCoverPhotoId(null);
    setPhotoDeleteMode(false);
    setSelectedDeletePhotoIds([]);
    setPhotoSelectMode((m) => {
      const next = !m;
      if (!next) setSelectedPhotoIds([]);
      return next;
    });
  }

  function togglePhotoDeleteMode() {
    setPhotoDeleteMessage(null);
    setPhotoSelectMessage(null);
    setCoverMessage(null);
    setCoverSelectMode(false);
    setSelectedCoverPhotoId(null);
    setPhotoSelectMode(false);
    setSelectedPhotoIds([]);
    setPhotoDeleteMode((m) => {
      const next = !m;
      if (!next) setSelectedDeletePhotoIds([]);
      return next;
    });
  }

  function toggleCoverSelectMode() {
    setCoverMessage(null);
    setPhotoSelectMessage(null);
    setPhotoDeleteMessage(null);
    setPhotoSelectMode(false);
    setSelectedPhotoIds([]);
    setPhotoDeleteMode(false);
    setSelectedDeletePhotoIds([]);
    setCoverSelectMode((m) => {
      const next = !m;
      if (!next) setSelectedCoverPhotoId(null);
      return next;
    });
  }

  async function handlePhotoSelectComplete() {
    setPhotoSelectMessage(null);
    if (selectedPhotoIds.length === 0) {
      setPhotoSelectMessage("채택할 사진을 하나 이상 선택하세요.");
      return;
    }
    const uid = bookUid.trim();
    if (!uid) {
      setPhotoSelectMessage("북 UID를 확인할 수 없습니다.");
      return;
    }
    setPhotoCompletePending(true);
    try {
      const photos: string[] = [];
      for (const id of selectedPhotoIds) {
        const photo = localPhotos.find((p) => p.id === id);
        const name = photo?.sweetbookFileName?.trim();
        if (!name) {
          setPhotoSelectMessage(
            "Sweetbook 파일명이 없는 사진이 있습니다. 먼저 업로드 페이지에서 이 북으로 사진을 올린 뒤 다시 선택하세요."
          );
          return;
        }
        photos.push(name);
      }

      const monthYearLabel = new Date()
        .toLocaleDateString("en-CA", { timeZone: "Asia/Seoul" })
        .slice(0, 7);

      let singleMsg = "책 콘텐츠가 추가되었습니다.";
      let lastPageCount: number | undefined;
      for (let i = 0; i < photos.length; i++) {
        const res = await postBookContents(uid, {
          templateUid: SWEETBOOK_DEFAULT_CONTENTS_TEMPLATE_UID,
          parameters: {
            monthYearLabel,
            photos: [photos[i]],
          },
        });
        const text = await res.text();
        if (!res.ok) {
          const detail = resolvePhotoSelectFailureMessage(text, res.status);
          setPhotoSelectMessage(
            photos.length > 1
              ? `${i + 1}번째 사진(${photos[i]}) 콘텐츠 추가 실패: ${detail}`
              : detail
          );
          return;
        }
        try {
          const j = JSON.parse(text) as AddBookContentsResponse;
          if (photos.length === 1 && j.message) singleMsg = j.message;
          if (j.data?.pageCount != null) lastPageCount = j.data.pageCount;
        } catch {
          /* keep */
        }
      }

      let msg =
        photos.length > 1
          ? `${photos.length}장의 사진에 대해 콘텐츠가 추가되었습니다.`
          : singleMsg;
      if (lastPageCount != null) {
        msg = `${msg} (${lastPageCount}페이지)`;
      }
      const idsOrdered = [...selectedPhotoIds];
      const selRes = await appendBookSelection(uid, idsOrdered);
      const selText = await selRes.text();
      if (!selRes.ok) {
        setPhotoSelectMessage(
          `${msg}\n(책 미리보기용 채택 목록 저장 실패: ${selText || selRes.status})`
        );
        setPhotoSelectMode(false);
        setSelectedPhotoIds([]);
        await reloadSelectedPhotosForBook();
        return;
      }
      setPhotoSelectMode(false);
      setSelectedPhotoIds([]);
      window.location.reload();
    } catch {
      setPhotoSelectMessage("네트워크 오류로 콘텐츠를 추가할 수 없습니다.");
    } finally {
      setPhotoCompletePending(false);
    }
  }

  async function reloadSelectedPhotosForBook() {
    const uid = bookUid.trim();
    if (!uid) return;
    const sRes = await fetchSelectedPhotosForBook(uid);
    const sText = await sRes.text();
    if (sRes.ok) {
      try {
        const arr = JSON.parse(sText) as LocalPhotoItem[];
        if (Array.isArray(arr)) {
          setSelectedFlipPhotos(arr);
        }
      } catch {
        /* 이전 목록 유지 (낙관적 갱신·파싱 실패 시 빈 화면 방지) */
      }
    }
  }

  async function reloadLocalPhotosForBook() {
    const uid = bookUid.trim();
    if (!uid) return;
    const loaded = await loadUploadedPhotosViaSweetbook(uid);
    setLocalPhotos(loaded.photos);
    setLocalLoadError(loaded.errorMessage);
    await reloadSelectedPhotosForBook();
  }

  async function handlePhotoDeleteComplete() {
    setPhotoDeleteMessage(null);
    if (selectedDeletePhotoIds.length === 0) {
      setPhotoDeleteMessage("삭제할 사진을 하나 이상 선택하세요.");
      return;
    }
    const uid = bookUid.trim();
    if (!uid) {
      setPhotoDeleteMessage("북 UID를 확인할 수 없습니다.");
      return;
    }
    setPhotoDeletePending(true);
    try {
      for (const id of selectedDeletePhotoIds) {
        const photo = localPhotos.find((p) => p.id === id);
        const name = photo?.sweetbookFileName?.trim();
        if (!name) {
          setPhotoDeleteMessage(
            "Sweetbook 파일명이 없는 사진이 있습니다. 업로드된 사진만 삭제할 수 있습니다."
          );
          return;
        }
        const res = await deleteBookPhoto(uid, name);
        const text = await res.text();
        if (!res.ok) {
          let detail = text || `삭제 실패 (${res.status})`;
          try {
            const j = JSON.parse(text) as { message?: string };
            if (j?.message) detail = j.message;
          } catch {
            /* keep */
          }
          setPhotoDeleteMessage(detail);
          return;
        }
      }
      setPhotoDeleteMessage("선택한 사진을 삭제했습니다.");
      setPhotoDeleteMode(false);
      setSelectedDeletePhotoIds([]);
      await reloadLocalPhotosForBook();
    } catch {
      setPhotoDeleteMessage("네트워크 오류로 삭제할 수 없습니다.");
    } finally {
      setPhotoDeletePending(false);
    }
  }

  async function handleCoverComplete() {
    setCoverMessage(null);
    if (selectedCoverPhotoId == null) {
      setCoverMessage("표지로 사용할 사진을 하나 선택하세요.");
      return;
    }
    const photo = localPhotos.find((p) => p.id === selectedCoverPhotoId);
    if (!photo) return;

    const coverBookUid =
      typeof photo.bookUid === "string" && photo.bookUid.trim() !== ""
        ? photo.bookUid.trim()
        : bookUid.trim();
    if (!coverBookUid) {
      setCoverMessage("북 UID를 확인할 수 없습니다.");
      return;
    }

    setCoverSavePending(true);
    try {
      const imageUrl = localPhotoAbsoluteUrl(photo.fileUrl);
      const imgRes = await fetch(imageUrl, { method: "GET", credentials: "include" });
      if (!imgRes.ok) {
        setCoverMessage(`표지 이미지를 불러올 수 없습니다. (${imgRes.status})`);
        return;
      }
      const blob = await imgRes.blob();
      const baseName = photo.originalName?.trim() || `cover-${photo.id}.jpg`;
      const mime = blob.type || photo.mimeType || "image/jpeg";
      const imageFile = new File([blob], baseName, { type: mime });
      const coverRes = await uploadBookCover(coverBookUid, imageFile, {
        parameters: {
          title: coverBookUid,
          author: "",
          subtitle: "",
          dateRange: "",
        },
      });
      const coverText = await coverRes.text();
      if (!coverRes.ok) {
        let detail = coverText || `표지 등록 실패 (${coverRes.status})`;
        try {
          const j = JSON.parse(coverText) as { message?: string };
          if (j.message) detail = j.message;
        } catch {
          /* keep detail */
        }
        setCoverMessage(detail);
        return;
      }
      let msg = "표지가 등록되었습니다.";
      try {
        const j = JSON.parse(coverText) as UploadBookCoverResponse;
        if (j.message) msg = j.message;
      } catch {
        /* keep default msg */
      }
      try {
        const saveRes = await saveBookCover(coverBookUid, { photoId: photo.id });
        if (!saveRes.ok) {
          msg = `${msg} (메인 썸네일 동기화는 실패했습니다.)`;
        }
      } catch {
        msg = `${msg} (메인 썸네일 동기화는 실패했습니다.)`;
      }
      setCoverMessage(msg);
      setCoverSelectMode(false);
      setSelectedCoverPhotoId(null);
    } catch {
      setCoverMessage("네트워크 오류로 표지를 등록할 수 없습니다.");
    } finally {
      setCoverSavePending(false);
    }
  }

  const ownerMayEditPhotos = isOwnerOfThisBook && !bookFinalized;
  const showPurchaseCta =
    galleryFinalized && hasBookSection && !isOwnerOfThisBook;
  const purchasePagePath =
    bookUid.trim() === ""
      ? ""
      : `/book/${encodeURIComponent(bookUid.trim())}/purchase`;
  const purchaseButtonHref =
    !purchasePagePath
      ? "/login"
      : loggedIn
        ? purchasePagePath
        : `/login?next=${encodeURIComponent(purchasePagePath)}`;

  const uploadPagePath =
    bookUid.trim() === ""
      ? ""
      : `/upload?bookUid=${encodeURIComponent(bookUid.trim())}`;
  const uploadButtonHref =
    uploadPagePath === ""
      ? "/login"
      : loggedIn
        ? uploadPagePath
        : `/login?next=${encodeURIComponent(uploadPagePath)}`;

  const bookDisplayPriceWon = useMemo(() => {
    for (const p of localPhotos) {
      const v = p.price;
      if (typeof v === "number" && Number.isFinite(v) && v >= 0) {
        return v;
      }
    }
    return null;
  }, [localPhotos]);

  async function submitFinishEditing() {
    const uid = bookUid.trim();
    if (!uid) return;
    setFinalizePending(true);
    setFinalizedBannerMessage(null);
    setFinalizeBannerIsError(false);
    try {
      const res = await postBookFinalization(uid, 0);
      const text = await res.text();
      let j: FinalizeBookResponse = {};
      try {
        j = text ? (JSON.parse(text) as FinalizeBookResponse) : {};
      } catch {
        /* keep */
      }
      if (!res.ok) {
        setFinalizeBannerIsError(true);
        setFinalizedBannerMessage(resolveFinalizeFailureMessage(j, text));
        return;
      }
      if (j.success !== true) {
        setFinalizeBannerIsError(true);
        setFinalizedBannerMessage(resolveFinalizeFailureMessage(j, text));
        return;
      }
      setFinalizeBannerIsError(false);
      setFinalizedBannerMessage(
        typeof j.message === "string" && j.message.trim() !== ""
          ? j.message
          : "편집을 마쳤습니다."
      );
      setBookFinalized(true);
      const entriesRes = await fetchMyBookEntries();
      if (entriesRes.ok) {
        try {
          const entries = (await entriesRes.json()) as MyBookEntry[];
          const entry = Array.isArray(entries)
            ? entries.find((e) => e.bookUid === uid)
            : undefined;
          if (entry?.finalized === true) {
            setBookFinalized(true);
          }
        } catch {
          /* keep setBookFinalized(true) from above */
        }
      }
    } catch {
      setFinalizeBannerIsError(true);
      setFinalizedBannerMessage("네트워크 오류로 최종화할 수 없습니다.");
    } finally {
      setFinalizePending(false);
    }
  }

  const effectiveCoverSelectMode = ownerMayEditPhotos && coverSelectMode;
  const effectivePhotoSelectMode = ownerMayEditPhotos && photoSelectMode;
  const effectivePhotoDeleteMode = ownerMayEditPhotos && photoDeleteMode;

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-[min(100%,1600px)] px-4 py-8">
        <h1 className="mb-1 font-mono text-sm text-zinc-500">{bookUid || "—"}</h1>
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-lg font-semibold">책 사진 목록</p>
          {bookUid.trim() !== "" ? (
            <Link
              href={uploadButtonHref}
              className="inline-flex w-fit shrink-0 items-center rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              사진 업로드
            </Link>
          ) : null}
        </div>

        {loading ? <p className="text-sm text-zinc-500">불러오는 중…</p> : null}
        {error ? (
          <p className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
            {error}
          </p>
        ) : null}
        {sweetbookError ? (
          <p className="mb-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
            {sweetbookError}
          </p>
        ) : null}
        {localLoadError && !error ? (
          <p className="mb-4 text-sm text-amber-700 dark:text-amber-300">{localLoadError}</p>
        ) : null}
        {isOwnerOfThisBook && finalizedBannerMessage ? (
          <p
            className={
              finalizeBannerIsError
                ? "mb-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200"
                : "mb-4 rounded-md border border-sky-200 bg-sky-50 p-3 text-sm text-sky-900 dark:border-sky-900 dark:bg-sky-950 dark:text-sky-200"
            }
          >
            {finalizedBannerMessage}
          </p>
        ) : null}

        {!loading && !error && hasBookSection ? (
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-[8fr_2fr] lg:items-start lg:gap-6">
            <div className="min-w-0">
              <section aria-label="책 넘김 보기 (사진 채택된 이미지)">
                {selectedFlipPhotos.length > 0 ? (
                  <BookPageFlip photos={selectedFlipPhotos} expanded />
                ) : (
                  <div className="rounded-lg border border-dashed border-zinc-300 bg-zinc-50 p-8 text-center text-sm text-zinc-600 dark:border-zinc-600 dark:bg-zinc-900/40 dark:text-zinc-400">
                    사진 채택으로 선택한 이미지가 없습니다. 오른쪽에서 채택 후 선택 완료를 누르면 여기
                    책 미리보기에 표시됩니다.
                  </div>
                )}
              </section>
            </div>
            <aside
              aria-label="업로드된 모든 사진"
              className="min-w-0 border-t border-zinc-200 pt-6 dark:border-zinc-800 lg:border-l lg:border-t-0 lg:pl-6 lg:pr-6 lg:pt-0"
            >
              {showPurchaseCta ? (
                <div className="mb-4 space-y-2">
                  <p className="text-center text-sm text-zinc-600 dark:text-zinc-400">
                    {bookDisplayPriceWon !== null ? (
                      <>
                        <span className="text-xs text-zinc-500 dark:text-zinc-500">판매가</span>
                        <br />
                        <span className="text-lg font-semibold tabular-nums text-zinc-900 dark:text-zinc-100">
                          {bookDisplayPriceWon.toLocaleString("ko-KR")}
                        </span>
                        <span className="text-sm font-medium text-zinc-700 dark:text-zinc-300">원</span>
                      </>
                    ) : (
                      <span className="text-zinc-500">가격 정보 없음</span>
                    )}
                  </p>
                  <Link
                    href={purchaseButtonHref}
                    className="inline-flex w-full items-center justify-center rounded-md border border-violet-700 bg-violet-700 px-3 py-2 text-sm font-medium text-white hover:bg-violet-800 dark:border-violet-600 dark:bg-violet-800 dark:hover:bg-violet-700"
                  >
                    구매
                  </Link>
                </div>
              ) : null}
              <div className="mb-3 space-y-2">
                <h2 className="text-sm font-medium text-zinc-600 dark:text-zinc-400">
                  업로드 사진 (클릭 시 이 기기에 저장)
                </h2>
                <div className="flex flex-wrap items-center gap-3">
                  {ownerMayEditPhotos ? (
                    <>
                      <button
                        type="button"
                        disabled={finalizePending}
                        onClick={() => void submitFinishEditing()}
                        className="rounded-md border border-emerald-700 bg-emerald-700 px-2.5 py-1 text-xs font-medium text-white hover:bg-emerald-800 disabled:opacity-50 dark:border-emerald-600 dark:bg-emerald-800 dark:hover:bg-emerald-700"
                      >
                        {finalizePending ? "처리 중…" : "편집 마치기"}
                      </button>
                      <button
                        type="button"
                        aria-pressed={photoSelectMode}
                        onClick={togglePhotoSelectMode}
                        className={`rounded-md border px-2.5 py-1 text-xs font-medium dark:border-zinc-600 ${
                          photoSelectMode
                            ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                            : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50 dark:bg-zinc-950 dark:text-zinc-200 dark:hover:bg-zinc-900"
                        }`}
                      >
                        사진 채택
                      </button>
                      {photoSelectMode ? (
                        <button
                          type="button"
                          disabled={photoCompletePending}
                          onClick={() => void handlePhotoSelectComplete()}
                          className="rounded-md bg-zinc-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-zinc-800 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
                        >
                          {photoCompletePending ? "저장 중…" : "선택 완료"}
                        </button>
                      ) : null}
                      <button
                        type="button"
                        aria-pressed={photoDeleteMode}
                        onClick={togglePhotoDeleteMode}
                        className={`rounded-md border px-2.5 py-1 text-xs font-medium dark:border-zinc-600 ${
                          photoDeleteMode
                            ? "border-red-800 bg-red-800 text-white dark:border-red-700 dark:bg-red-900"
                            : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50 dark:bg-zinc-950 dark:text-zinc-200 dark:hover:bg-zinc-900"
                        }`}
                      >
                        삭제
                      </button>
                      {photoDeleteMode ? (
                        <button
                          type="button"
                          disabled={photoDeletePending}
                          onClick={() => void handlePhotoDeleteComplete()}
                          className="rounded-md bg-red-800 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-900 disabled:opacity-50 dark:bg-red-900 dark:hover:bg-red-800"
                        >
                          {photoDeletePending ? "삭제 중…" : "선택 완료"}
                        </button>
                      ) : null}
                      <button
                        type="button"
                        aria-pressed={coverSelectMode}
                        onClick={toggleCoverSelectMode}
                        className={`rounded-md border px-2.5 py-1 text-xs font-medium dark:border-zinc-600 ${
                          coverSelectMode
                            ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                            : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50 dark:bg-zinc-950 dark:text-zinc-200 dark:hover:bg-zinc-900"
                        }`}
                      >
                        표지 선택
                      </button>
                      {coverSelectMode ? (
                        <button
                          type="button"
                          disabled={coverSavePending}
                          onClick={() => void handleCoverComplete()}
                          className="rounded-md bg-zinc-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-zinc-800 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
                        >
                          {coverSavePending ? "저장 중…" : "선택 완료"}
                        </button>
                      ) : null}
                    </>
                  ) : null}
                  {isOwnerOfThisBook && bookFinalized && bookUid.trim() !== "" ? (
                    <Link
                      href={`/book/${encodeURIComponent(bookUid.trim())}/estimate`}
                      className="inline-flex items-center rounded-md border border-violet-700 bg-violet-700 px-2.5 py-1 text-xs font-medium text-white hover:bg-violet-800 dark:border-violet-600 dark:bg-violet-800 dark:hover:bg-violet-700"
                    >
                      견적 조회
                    </Link>
                  ) : null}
                </div>
                {ownerMayEditPhotos && photoSelectMessage ? (
                  <p className="text-xs text-zinc-600 dark:text-zinc-400">{photoSelectMessage}</p>
                ) : null}
                {ownerMayEditPhotos && photoDeleteMessage ? (
                  <p className="text-xs text-zinc-600 dark:text-zinc-400">{photoDeleteMessage}</p>
                ) : null}
                {ownerMayEditPhotos && coverMessage ? (
                  <p className="text-xs text-zinc-600 dark:text-zinc-400">{coverMessage}</p>
                ) : null}
              </div>
              <div className="lg:sticky lg:top-24 lg:max-h-[calc(100vh-7rem)] lg:overflow-y-auto">
                <ul className="grid grid-cols-2 gap-2">
                  {localPhotos.map((p) => (
                    <li
                      key={p.id}
                      className="relative overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700"
                    >
                      {effectiveCoverSelectMode ? (
                        <div className="pointer-events-auto absolute left-1 top-1 z-10 rounded bg-white/90 p-0.5 shadow dark:bg-zinc-900/90">
                          <input
                            type="checkbox"
                            checked={selectedCoverPhotoId === p.id}
                            onChange={() =>
                              setSelectedCoverPhotoId((cur) =>
                                cur === p.id ? null : p.id
                              )
                            }
                            aria-label={`표지로 선택: ${p.originalName || p.id}`}
                            className="h-4 w-4 rounded border-zinc-300 text-zinc-900 focus:ring-2 focus:ring-zinc-400 dark:border-zinc-600 dark:bg-zinc-950 dark:focus:ring-zinc-500"
                          />
                        </div>
                      ) : null}
                      {effectivePhotoSelectMode ? (
                        <div className="pointer-events-auto absolute left-1 top-1 z-10 rounded bg-white/90 p-0.5 shadow dark:bg-zinc-900/90">
                          <input
                            type="checkbox"
                            checked={selectedPhotoIds.includes(p.id)}
                            onChange={() =>
                              setSelectedPhotoIds((prev) =>
                                prev.includes(p.id)
                                  ? prev.filter((x) => x !== p.id)
                                  : [...prev, p.id]
                              )
                            }
                            aria-label={`책에 채택할 사진 선택: ${p.originalName || p.id}`}
                            className="h-4 w-4 rounded border-zinc-300 text-zinc-900 focus:ring-2 focus:ring-zinc-400 dark:border-zinc-600 dark:bg-zinc-950 dark:focus:ring-zinc-500"
                          />
                        </div>
                      ) : null}
                      {effectivePhotoDeleteMode ? (
                        <div className="pointer-events-auto absolute left-1 top-1 z-10 rounded bg-white/90 p-0.5 shadow dark:bg-zinc-900/90">
                          <input
                            type="checkbox"
                            checked={selectedDeletePhotoIds.includes(p.id)}
                            onChange={() =>
                              setSelectedDeletePhotoIds((prev) =>
                                prev.includes(p.id)
                                  ? prev.filter((x) => x !== p.id)
                                  : [...prev, p.id]
                              )
                            }
                            aria-label={`삭제할 사진 선택: ${p.originalName || p.id}`}
                            className="h-4 w-4 rounded border-zinc-300 text-red-800 focus:ring-2 focus:ring-red-400 dark:border-zinc-600 dark:bg-zinc-950 dark:focus:ring-red-500"
                          />
                        </div>
                      ) : null}
                      <button
                        type="button"
                        title={
                          effectiveCoverSelectMode
                            ? "클릭하여 표지 후보 선택"
                            : effectivePhotoSelectMode
                              ? "클릭하여 채택할 사진 선택"
                              : effectivePhotoDeleteMode
                                ? "클릭하여 삭제할 사진 선택"
                                : "클릭하여 이 기기에 저장"
                        }
                        className="relative block w-full cursor-pointer rounded-lg p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                        onClick={() => {
                          if (effectiveCoverSelectMode) {
                            setSelectedCoverPhotoId((cur) =>
                              cur === p.id ? null : p.id
                            );
                            return;
                          }
                          if (effectivePhotoSelectMode) {
                            setSelectedPhotoIds((prev) =>
                              prev.includes(p.id)
                                ? prev.filter((x) => x !== p.id)
                                : [...prev, p.id]
                            );
                            return;
                          }
                          if (effectivePhotoDeleteMode) {
                            setSelectedDeletePhotoIds((prev) =>
                              prev.includes(p.id)
                                ? prev.filter((x) => x !== p.id)
                                : [...prev, p.id]
                            );
                            return;
                          }
                          void downloadLocalPhoto(
                            p.fileUrl,
                            p.originalName || `photo-${p.id}`
                          ).catch(() => {
                            alert("이미지를 저장할 수 없습니다.");
                          });
                        }}
                      >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                          src={localPhotoAbsoluteUrl(p.fileUrl)}
                          alt={p.originalName}
                          className={`aspect-square w-full object-cover bg-zinc-100 dark:bg-zinc-900 ${
                            effectiveCoverSelectMode ||
                            effectivePhotoSelectMode ||
                            effectivePhotoDeleteMode
                              ? ""
                              : "pointer-events-none"
                          }`}
                        />
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            </aside>
          </div>
        ) : null}

        {!loading && !error && !hasBookSection && !localLoadError ? (
          <p className="text-sm text-zinc-500">표시할 사진이 없습니다.</p>
        ) : null}
      </main>
    </div>
  );
}
