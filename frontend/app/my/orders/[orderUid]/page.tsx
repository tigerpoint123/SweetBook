"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import {
  fetchV1OrderDetail,
  patchV1OrderShipping,
  postV1OrderCancel,
  type OrderDetailEnvelope,
} from "@/lib/order-api";

function amount(n: unknown): string {
  const num = typeof n === "number" ? n : Number(n);
  if (!Number.isFinite(num)) return "-";
  return `${num.toLocaleString("ko-KR", { maximumFractionDigits: 2 })}원`;
}

export default function OrderDetailPage() {
  const params = useParams();
  const router = useRouter();
  const orderUid = typeof params.orderUid === "string" ? params.orderUid : "";
  const [loggedIn, setLoggedIn] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [payload, setPayload] = useState<OrderDetailEnvelope | null>(null);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelPending, setCancelPending] = useState(false);
  const [cancelMessage, setCancelMessage] = useState<string | null>(null);
  const [shippingOpen, setShippingOpen] = useState(false);
  const [shippingRecipientName, setShippingRecipientName] = useState("");
  const [shippingAddress1, setShippingAddress1] = useState("");
  const [shippingPending, setShippingPending] = useState(false);
  const [shippingMessage, setShippingMessage] = useState<string | null>(null);

  const loginNext = useMemo(() => {
    const uid = orderUid.trim();
    return uid
      ? `/login?next=${encodeURIComponent(`/my/orders/${encodeURIComponent(uid)}`)}`
      : "/login";
  }, [orderUid]);

  useEffect(() => {
    const ok = readLoggedIn();
    setLoggedIn(ok);
    if (!ok) {
      setLoading(false);
      return;
    }
    const uid = orderUid.trim();
    if (!uid) {
      setError("주문 UID가 없습니다.");
      setLoading(false);
      return;
    }
    let alive = true;
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchV1OrderDetail(uid);
        const text = await res.text();
        let body: OrderDetailEnvelope = {};
        try {
          body = text ? (JSON.parse(text) as OrderDetailEnvelope) : {};
        } catch {
          // keep empty body
        }
        if (!alive) return;
        if (res.status === 401) {
          setError("로그인이 필요합니다.");
          return;
        }
        if (!res.ok) {
          setError(body.message || text || `주문 상세 조회 실패 (${res.status})`);
          return;
        }
        if (body.success === false) {
          setError(body.message || "주문 상세 조회에 실패했습니다.");
          return;
        }
        setPayload(body);
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [orderUid]);

  const data = payload?.data;
  const items = Array.isArray(data?.items) ? data.items : [];

  async function handleCancelOrder() {
    const uid = orderUid.trim();
    if (!uid || cancelPending) return;
    const reason = cancelReason.trim();
    if (!reason) {
      setCancelMessage("취소 사유를 입력해 주세요.");
      return;
    }
    if (reason.length > 500) {
      setCancelMessage("취소 사유는 500자 이하여야 합니다.");
      return;
    }
    setCancelPending(true);
    setCancelMessage(null);
    try {
      const res = await postV1OrderCancel(uid, reason);
      const text = await res.text();
      let body: OrderDetailEnvelope = {};
      try {
        body = text ? (JSON.parse(text) as OrderDetailEnvelope) : {};
      } catch {
        // keep empty body
      }
      if (res.status === 401) {
        setCancelMessage("로그인이 필요합니다.");
        return;
      }
      if (!res.ok) {
        setCancelMessage(body.message || text || `주문 취소 실패 (${res.status})`);
        return;
      }
      if (body.success === false) {
        setCancelMessage(body.message || "주문 취소에 실패했습니다.");
        return;
      }
      setCancelMessage(body.message || "주문 취소가 완료되었습니다.");
      setCancelOpen(false);
      setCancelReason("");
      const detailRes = await fetchV1OrderDetail(uid);
      const detailText = await detailRes.text();
      let detailBody: OrderDetailEnvelope = {};
      try {
        detailBody = detailText ? (JSON.parse(detailText) as OrderDetailEnvelope) : {};
      } catch {
        // keep empty body
      }
      if (detailRes.ok) {
        setPayload(detailBody);
      }
    } finally {
      setCancelPending(false);
    }
  }

  async function refreshOrderDetail(uid: string) {
    const detailRes = await fetchV1OrderDetail(uid);
    const detailText = await detailRes.text();
    let detailBody: OrderDetailEnvelope = {};
    try {
      detailBody = detailText ? (JSON.parse(detailText) as OrderDetailEnvelope) : {};
    } catch {
      // keep empty body
    }
    if (detailRes.ok) {
      setPayload(detailBody);
    }
  }

  async function handleUpdateShipping() {
    const uid = orderUid.trim();
    if (!uid || shippingPending) return;
    const recipientName = shippingRecipientName.trim();
    const address1 = shippingAddress1.trim();
    if (!recipientName) {
      setShippingMessage("수령인을 입력해 주세요.");
      return;
    }
    if (!address1) {
      setShippingMessage("주소1을 입력해 주세요.");
      return;
    }
    setShippingPending(true);
    setShippingMessage(null);
    try {
      const res = await patchV1OrderShipping(uid, recipientName, address1);
      const text = await res.text();
      let body: OrderDetailEnvelope = {};
      try {
        body = text ? (JSON.parse(text) as OrderDetailEnvelope) : {};
      } catch {
        // keep empty body
      }
      if (res.status === 401) {
        setShippingMessage("로그인이 필요합니다.");
        return;
      }
      if (!res.ok) {
        setShippingMessage(body.message || text || `배송지 변경 실패 (${res.status})`);
        return;
      }
      if (body.success === false) {
        setShippingMessage(body.message || "배송지 변경에 실패했습니다.");
        return;
      }
      setShippingMessage(body.message || "배송지 변경이 완료되었습니다.");
      setShippingOpen(false);
      await refreshOrderDetail(uid);
    } finally {
      setShippingPending(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-4xl p-6">
      <h1 className="mb-4 text-xl font-semibold">주문 상세</h1>

      {!loggedIn ? (
        <section className="rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <p className="mb-3 text-sm text-zinc-600 dark:text-zinc-400">
            로그인 후 주문 상세를 확인할 수 있습니다.
          </p>
          <Link className="text-sm text-blue-600 underline" href={loginNext}>
            로그인 페이지로 이동
          </Link>
        </section>
      ) : loading ? (
        <p className="text-sm text-zinc-500">주문 상세를 불러오는 중...</p>
      ) : error ? (
        <section className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
          {error}
        </section>
      ) : (
        <>
          <section className="mb-6 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
            <div className="grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
              <p><span className="text-zinc-500">주문 UID</span> {data?.orderUid ?? "-"}</p>
              <p><span className="text-zinc-500">주문유형</span> {data?.orderType ?? "-"}</p>
              <p><span className="text-zinc-500">상태</span> {data?.orderStatus} {data?.orderStatusDisplay ?? ""}</p>
              <p><span className="text-zinc-500">외부참조</span> {data?.externalRef ?? "-"}</p>
              <p><span className="text-zinc-500">상품금액</span> {amount(data?.totalProductAmount)}</p>
              <p><span className="text-zinc-500">배송비</span> {amount(data?.totalShippingFee)}</p>
              <p><span className="text-zinc-500">포장비</span> {amount(data?.totalPackagingFee)}</p>
              <p><span className="text-zinc-500">총 금액</span> {amount(data?.totalAmount)}</p>
              <p><span className="text-zinc-500">총 지불 금액</span> {amount(data?.paidCreditAmount)}</p>
              <p><span className="text-zinc-500">주문시각</span> {data?.orderedAt ? new Date(data.orderedAt).toLocaleString("ko-KR") : "-"}</p>
            </div>
          </section>

          <section className="mb-6 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
            <h2 className="mb-3 text-base font-semibold">배송지 정보</h2>
            <div className="grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
              <p><span className="text-zinc-500">수령인</span> {data?.recipientName ?? "-"}</p>
              <p><span className="text-zinc-500">연락처</span> {data?.recipientPhone ?? "-"}</p>
              <p><span className="text-zinc-500">우편번호</span> {data?.postalCode ?? "-"}</p>
              <p><span className="text-zinc-500">주소1</span> {data?.address1 ?? "-"}</p>
              <p><span className="text-zinc-500">주소2</span> {data?.address2 ?? "-"}</p>
            </div>
          </section>

          <section className="mb-6 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
            <h2 className="mb-3 text-base font-semibold">주문 품목</h2>
            {items.length === 0 ? (
              <p className="text-sm text-zinc-500">품목 정보가 없습니다.</p>
            ) : (
              <div className="overflow-x-auto rounded-lg border border-zinc-200 dark:border-zinc-700">
                <table className="w-full min-w-[680px] text-left text-sm">
                  <thead className="border-b border-zinc-200 bg-zinc-50 text-xs text-zinc-600 dark:border-zinc-700 dark:bg-zinc-900/60 dark:text-zinc-400">
                    <tr>
                      <th className="px-3 py-2 font-medium">품목 UID</th>
                      <th className="px-3 py-2 font-medium">책 UID</th>
                      <th className="px-3 py-2 font-medium">책 제목</th>
                      <th className="px-3 py-2 font-medium">수량</th>
                      <th className="px-3 py-2 font-medium">단가</th>
                      <th className="px-3 py-2 font-medium">금액</th>
                      <th className="px-3 py-2 font-medium">상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((it, idx) => (
                      <tr key={`${it.itemUid ?? "item"}-${idx}`} className="border-b border-zinc-100 dark:border-zinc-800">
                        <td className="px-3 py-2 font-mono text-xs">{it.itemUid ?? "-"}</td>
                        <td className="px-3 py-2 font-mono text-xs">{it.bookUid ?? "-"}</td>
                        <td className="px-3 py-2">{it.bookTitle ?? "-"}</td>
                        <td className="px-3 py-2">{it.quantity ?? "-"}</td>
                        <td className="px-3 py-2 tabular-nums">{amount(it.unitPrice)}</td>
                        <td className="px-3 py-2 tabular-nums">{amount(it.itemAmount)}</td>
                        <td className="px-3 py-2">
                          <span className="text-xs text-zinc-500">{it.itemStatus}</span>{" "}
                          {it.itemStatusDisplay ?? ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => {
                setCancelOpen(true);
                setShippingOpen(false);
              }}
              className="rounded-md border border-zinc-300 px-3 py-2 text-sm hover:bg-zinc-50 dark:border-zinc-600 dark:hover:bg-zinc-900"
            >
              주문 취소
            </button>
            <button
              type="button"
              onClick={() => {
                setShippingRecipientName(data?.recipientName ?? "");
                setShippingAddress1(data?.address1 ?? "");
                setShippingOpen(true);
                setCancelOpen(false);
              }}
              className="rounded-md border border-zinc-300 px-3 py-2 text-sm hover:bg-zinc-50 dark:border-zinc-600 dark:hover:bg-zinc-900"
            >
              배송지 변경
            </button>
            <button
              type="button"
              onClick={() => router.push("/my")}
              className="ml-auto rounded-md bg-zinc-900 px-3 py-2 text-sm text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              내 정보로 돌아가기
            </button>
          </section>
          {cancelMessage ? (
            <p className="mt-3 text-sm text-zinc-700 dark:text-zinc-300">{cancelMessage}</p>
          ) : null}
          {shippingMessage ? (
            <p className="mt-3 text-sm text-zinc-700 dark:text-zinc-300">{shippingMessage}</p>
          ) : null}
          {cancelOpen ? (
            <section className="mt-4 rounded-md border border-zinc-200 p-4 dark:border-zinc-700">
              <h3 className="mb-2 text-sm font-semibold">취소 사유 입력</h3>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                maxLength={500}
                rows={5}
                placeholder="취소 사유를 입력하세요. (최대 500자)"
                className="w-full rounded-md border border-zinc-300 bg-white p-2 text-sm dark:border-zinc-700 dark:bg-zinc-950"
              />
              <p className="mt-1 text-right text-xs text-zinc-500">
                {cancelReason.length}/500
              </p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  disabled={cancelPending}
                  onClick={() => void handleCancelOrder()}
                  className="rounded-md bg-red-700 px-3 py-2 text-sm text-white hover:bg-red-800 disabled:opacity-60"
                >
                  {cancelPending ? "취소 처리 중..." : "취소 완료"}
                </button>
                <button
                  type="button"
                  disabled={cancelPending}
                  onClick={() => {
                    setCancelOpen(false);
                    setCancelReason("");
                  }}
                  className="rounded-md border border-zinc-300 px-3 py-2 text-sm hover:bg-zinc-50 disabled:opacity-60 dark:border-zinc-600 dark:hover:bg-zinc-900"
                >
                  닫기
                </button>
              </div>
            </section>
          ) : null}
          {shippingOpen ? (
            <section className="mt-4 rounded-md border border-zinc-200 p-4 dark:border-zinc-700">
              <h3 className="mb-2 text-sm font-semibold">배송지 변경 입력</h3>
              <div className="grid grid-cols-1 gap-2 md:grid-cols-2">
                <label className="text-sm">
                  <span className="mb-1 block text-zinc-600 dark:text-zinc-400">수령인</span>
                  <input
                    type="text"
                    value={shippingRecipientName}
                    onChange={(e) => setShippingRecipientName(e.target.value)}
                    placeholder="김영희"
                    className="w-full rounded-md border border-zinc-300 bg-white p-2 text-sm dark:border-zinc-700 dark:bg-zinc-950"
                  />
                </label>
                <label className="text-sm md:col-span-2">
                  <span className="mb-1 block text-zinc-600 dark:text-zinc-400">주소1</span>
                  <input
                    type="text"
                    value={shippingAddress1}
                    onChange={(e) => setShippingAddress1(e.target.value)}
                    placeholder="서울시 서초구 반포대로 100"
                    className="w-full rounded-md border border-zinc-300 bg-white p-2 text-sm dark:border-zinc-700 dark:bg-zinc-950"
                  />
                </label>
              </div>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  disabled={shippingPending}
                  onClick={() => void handleUpdateShipping()}
                  className="rounded-md bg-blue-700 px-3 py-2 text-sm text-white hover:bg-blue-800 disabled:opacity-60"
                >
                  {shippingPending ? "변경 처리 중..." : "변경 완료"}
                </button>
                <button
                  type="button"
                  disabled={shippingPending}
                  onClick={() => {
                    setShippingOpen(false);
                  }}
                  className="rounded-md border border-zinc-300 px-3 py-2 text-sm hover:bg-zinc-50 disabled:opacity-60 dark:border-zinc-600 dark:hover:bg-zinc-900"
                >
                  닫기
                </button>
              </div>
            </section>
          ) : null}
        </>
      )}
    </main>
  );
}
