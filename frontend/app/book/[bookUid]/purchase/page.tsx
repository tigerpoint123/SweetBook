"use client";

import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import {
  postV1Order,
  type CreateOrderPayload,
  type CreateOrderResponse,
} from "@/lib/order-api";

export default function BookPurchasePage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const bookUid = typeof params.bookUid === "string" ? params.bookUid : "";

  const loginNext = useMemo(
    () =>
      bookUid.trim()
        ? `/login?next=${encodeURIComponent(`/book/${encodeURIComponent(bookUid.trim())}/purchase`)}`
        : "/login",
    [bookUid]
  );

  const initialQuantity = searchParams.get("quantity")?.trim() ?? "1";
  const estimatedTotalRaw = searchParams.get("estimateTotal")?.trim() ?? "";
  const estimatedTotal = Number.parseInt(estimatedTotalRaw, 10);
  const hasEstimatedTotal = Number.isFinite(estimatedTotal) && estimatedTotal >= 0;
  const [quantity, setQuantity] = useState(initialQuantity);
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [address1, setAddress1] = useState("");
  const [address2, setAddress2] = useState("");
  const [memo, setMemo] = useState("");
  const [externalRef, setExternalRef] = useState("");
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [orderResponse, setOrderResponse] = useState<CreateOrderResponse | null>(null);

  function hasInsufficientCreditError(parsed: CreateOrderResponse, rawText: string): boolean {
    const msg = typeof parsed.message === "string" ? parsed.message : "";
    if (msg.includes("Insufficient Credit") || msg.includes("잔액이 부족")) {
      return true;
    }
    const anyParsed = parsed as Record<string, unknown>;
    const errors = anyParsed.errors;
    if (
      Array.isArray(errors) &&
      errors.some((e) => typeof e === "string" && e.includes("잔액이 부족"))
    ) {
      return true;
    }
    return rawText.includes("Insufficient Credit") || rawText.includes("잔액이 부족");
  }

  useEffect(() => {
    setLoggedIn(readLoggedIn());
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    setOrderResponse(null);
    const uid = bookUid.trim();
    if (!uid) {
      setMessage("북 UID가 없습니다.");
      return;
    }
    if (!loggedIn) {
      setMessage("로그인 후 구매할 수 있습니다.");
      return;
    }
    const qty = Number.parseInt(quantity.trim(), 10);
    if (!Number.isFinite(qty) || qty < 1) {
      setMessage("수량은 1 이상의 정수로 입력하세요.");
      return;
    }
    const ref =
      externalRef.trim() ||
      `WEB-${typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : Date.now()}`;

    const payload: CreateOrderPayload = {
      items: [{ bookUid: uid, quantity: qty }],
      shipping: {
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        postalCode: postalCode.trim(),
        address1: address1.trim(),
        address2: address2.trim() || undefined,
        memo: memo.trim() || undefined,
      },
      externalRef: ref,
    };

    setPending(true);
    try {
      const res = await postV1Order(payload);
      const text = await res.text();
      let parsed: CreateOrderResponse = {};
      try {
        parsed = text ? (JSON.parse(text) as CreateOrderResponse) : {};
      } catch {
        /* keep */
      }
      console.log("[purchase] /v1/orders response", parsed);
      if (res.status === 401) {
        setMessage("로그인이 만료되었거나 인증되지 않았습니다. 다시 로그인해 주세요.");
        return;
      }
      if (!res.ok) {
        if (hasInsufficientCreditError(parsed, text)) {
          setMessage("잔액이 부족합니다. 내 정보 페이지로 이동합니다.");
          window.setTimeout(() => {
            router.push("/my");
          }, 1200);
          return;
        }
        setMessage(
          parsed.message ||
            text ||
            `주문 실패 (${res.status})`
        );
        return;
      }
      if (parsed.success !== true) {
        setMessage(parsed.message || "주문에 실패했습니다.");
        return;
      }
      setSuccess(true);
      setOrderResponse(parsed);
      const data = parsed.data;
      const orderUid =
        data && typeof data === "object" && "orderUid" in data
          ? (data as Record<string, unknown>).orderUid
          : null;
      setMessage(
        typeof orderUid === "string"
          ? `주문이 접수되었습니다. (주문 UID: ${orderUid})`
          : "주문이 접수되었습니다."
      );
    } catch {
      setMessage("네트워크 오류로 주문할 수 없습니다.");
    } finally {
      setPending(false);
    }
  }

  if (!bookUid.trim()) {
    return (
      <div className="min-h-screen p-6">
        <p className="text-sm text-zinc-600">북 UID가 없습니다.</p>
        <Link href="/" className="mt-4 inline-block text-sm underline">
          홈으로
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-lg px-4 py-8">
        <div className="mb-4 text-sm">
          <button
            type="button"
            onClick={() => router.push(`/book/${encodeURIComponent(bookUid.trim())}`)}
            className="hover:underline"
          >
            ← 책으로
          </button>
        </div>
        <h1 className="mb-2 text-xl font-semibold">구매 · 배송 정보</h1>
        <p className="mb-6 font-mono text-xs text-zinc-500">{bookUid.trim()}</p>

        {!loggedIn ? (
          <div className="mb-6 rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
            구매하려면 로그인이 필요합니다.{" "}
            <Link href={loginNext} className="font-medium underline">
              로그인
            </Link>
          </div>
        ) : null}

        {message ? (
          <p
            className={`mb-4 rounded-md border p-3 text-sm ${
              success
                ? "border-emerald-200 bg-emerald-50 text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-200"
                : "border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
            }`}
          >
            {message}
          </p>
        ) : null}

        {success ? (
          <div className="space-y-4">
            {orderResponse?.data ? (
              <div className="rounded-md border border-zinc-200 bg-zinc-50 p-4 text-sm dark:border-zinc-700 dark:bg-zinc-900/40">
                <h2 className="mb-3 text-base font-semibold">주문 생성 결과</h2>
                <dl className="space-y-2">
                  {Object.entries(orderResponse.data).map(([k, v]) => (
                    <div key={k} className="grid grid-cols-1 gap-1 sm:grid-cols-[11rem_1fr] sm:gap-3">
                      <dt className="text-zinc-600 dark:text-zinc-400">{k}</dt>
                      <dd className="break-words text-zinc-900 dark:text-zinc-100">
                        {Array.isArray(v) || (v !== null && typeof v === "object")
                          ? JSON.stringify(v)
                          : String(v)}
                      </dd>
                    </div>
                  ))}
                </dl>
              </div>
            ) : null}
            <button
              type="button"
              onClick={() => router.push(`/book/${encodeURIComponent(bookUid.trim())}`)}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900"
            >
              책 페이지로 돌아가기
            </button>
          </div>
        ) : (
          <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
            <div className="flex flex-wrap items-end gap-4">
              <label className="flex flex-col gap-1 text-sm">
                수량
                <input
                  type="number"
                  min={1}
                  step={1}
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                  disabled={pending || !loggedIn}
                  className="w-28 rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
                />
              </label>
              {hasEstimatedTotal ? (
                <p className="pb-2 text-sm text-zinc-700 dark:text-zinc-300">
                  총 금액:{" "}
                  <span className="font-semibold tabular-nums">
                    {estimatedTotal.toLocaleString("ko-KR")}원
                  </span>
                </p>
              ) : null}
            </div>
            <label className="flex flex-col gap-1 text-sm">
              수령인 이름
              <input
                value={recipientName}
                onChange={(e) => setRecipientName(e.target.value)}
                required
                disabled={pending || !loggedIn}
                autoComplete="name"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              수령인 전화
              <input
                value={recipientPhone}
                onChange={(e) => setRecipientPhone(e.target.value)}
                required
                disabled={pending || !loggedIn}
                autoComplete="tel"
                placeholder="010-1234-5678"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              우편번호
              <input
                value={postalCode}
                onChange={(e) => setPostalCode(e.target.value)}
                required
                disabled={pending || !loggedIn}
                autoComplete="postal-code"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              주소
              <input
                value={address1}
                onChange={(e) => setAddress1(e.target.value)}
                required
                disabled={pending || !loggedIn}
                autoComplete="address-line1"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              상세 주소 (선택)
              <input
                value={address2}
                onChange={(e) => setAddress2(e.target.value)}
                disabled={pending || !loggedIn}
                autoComplete="address-line2"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              배송 메모 (선택)
              <input
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
                disabled={pending || !loggedIn}
                placeholder="부재시 경비실"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              외부 참조 번호 (비우면 자동 생성)
              <input
                value={externalRef}
                onChange={(e) => setExternalRef(e.target.value)}
                disabled={pending || !loggedIn}
                placeholder="PARTNER-ORDER-001"
                className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <button
              type="submit"
              disabled={pending || !loggedIn}
              className="w-full rounded-md bg-zinc-900 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
            >
              {pending ? "처리 중…" : "구매하기"}
            </button>
          </form>
        )}
      </main>
    </div>
  );
}
