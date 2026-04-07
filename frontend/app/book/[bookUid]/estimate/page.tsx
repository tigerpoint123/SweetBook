"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { readLoggedIn } from "@/lib/auth-storage";
import { postV1OrdersEstimate } from "@/lib/order-api";
import { fetchMyBookEntries, type MyBookEntry } from "@/lib/sweetbook-api";

/** 견적 API 등에서 자주 쓰이는 필드명 → 한글 표시 */
const FIELD_LABELS: Record<string, string> = {
  success: "성공 여부",
  message: "메시지",
  data: "응답 데이터",
  items: "품목",
  pageCount: "페이지 수",
  quantity: "수량",
  unitPrice: "단가",
  itemAmount: "품목 금액",
  packagingFee: "포장비",
  productAmount: "상품 금액",
  shippingFee: "배송비",
  totalAmount: "합계 금액",
  paidCreditAmount: "총 지불 금액 (부가세 포함)",
  creditBalance: "크레딧 잔액",
  creditSufficient: "크레딧 충분 여부",
  currency: "통화",
};

function labelForKey(key: string): string {
  return FIELD_LABELS[key] ?? key;
}

function formatDisplayValue(key: string, value: unknown): string {
  if (value === null || value === undefined) {
    return "—";
  }
  if (typeof value === "boolean") {
    return value ? "예" : "아니오";
  }
  if (typeof value === "number") {
    if (
      key.toLowerCase().includes("amount") ||
      key.toLowerCase().includes("price") ||
      key.toLowerCase().includes("fee") ||
      key.toLowerCase().includes("balance") ||
      key.toLowerCase().includes("credit")
    ) {
      return `${value.toLocaleString("ko-KR")}`;
    }
    return String(value);
  }
  if (typeof value === "string") {
    return value;
  }
  return JSON.stringify(value);
}

function isPlainObject(v: unknown): v is Record<string, unknown> {
  return v !== null && typeof v === "object" && !Array.isArray(v);
}

function extractEstimatedPaidAmount(payload: unknown): number | null {
  if (!isPlainObject(payload)) return null;
  const data = payload.data;
  if (!isPlainObject(data)) return null;
  const paidCreditAmount = data.paidCreditAmount;
  return typeof paidCreditAmount === "number" && Number.isFinite(paidCreditAmount)
    ? paidCreditAmount
    : null;
}

function isScalarForRow(v: unknown): boolean {
  return (
    v === null ||
    typeof v === "string" ||
    typeof v === "number" ||
    typeof v === "boolean"
  );
}

function Row({ k, v }: { k: string; v: unknown }) {
  return (
    <div className="grid grid-cols-1 gap-1 border-b border-zinc-100 py-2 last:border-0 sm:grid-cols-[minmax(8rem,40%)_1fr] sm:gap-4 dark:border-zinc-800">
      <dt className="text-sm font-medium text-zinc-600 dark:text-zinc-400">{labelForKey(k)}</dt>
      <dd className="text-sm tabular-nums text-zinc-900 dark:text-zinc-100">{formatDisplayValue(k, v)}</dd>
    </div>
  );
}

const HIDDEN_ITEM_FIELDS = new Set(["bookUid", "bookSpecUid"]);

function ItemBlock({ index, item }: { index: number; item: Record<string, unknown> }) {
  const scalarEntries = Object.entries(item).filter(
    ([k, v]) => isScalarForRow(v) && !HIDDEN_ITEM_FIELDS.has(k)
  );
  if (scalarEntries.length === 0) {
    return null;
  }
  return (
    <div className="rounded-lg border border-zinc-200 bg-white p-3 dark:border-zinc-700 dark:bg-zinc-950/50">
      <p className="mb-2 text-xs font-semibold text-zinc-700 dark:text-zinc-300">품목 {index + 1}</p>
      <dl className="space-y-0">
        {scalarEntries.map(([k, v]) => (
          <Row key={k} k={k} v={v} />
        ))}
      </dl>
    </div>
  );
}

function DataBlock({ data }: { data: Record<string, unknown> }) {
  const itemsRaw = data.items;
  const rest = Object.entries(data).filter(
    ([k, v]) => k !== "items" && isScalarForRow(v)
  );

  return (
    <div className="space-y-4">
      {Array.isArray(itemsRaw) && itemsRaw.length > 0 ? (
        <section>
          <h3 className="mb-2 text-sm font-semibold text-zinc-800 dark:text-zinc-200">
            {labelForKey("items")}
          </h3>
          <div className="space-y-3">
            {itemsRaw.map((item, i) =>
              isPlainObject(item) ? (
                <ItemBlock key={i} index={i} item={item} />
              ) : (
                <p key={i} className="text-sm text-zinc-600">
                  {String(item)}
                </p>
              )
            )}
          </div>
        </section>
      ) : null}

      {rest.length > 0 ? (
        <section>
          <h3 className="mb-2 text-sm font-semibold text-zinc-800 dark:text-zinc-200">요약</h3>
          <dl className="rounded-lg border border-zinc-200 bg-zinc-50/80 p-3 dark:border-zinc-700 dark:bg-zinc-900/40">
            {rest.map(([k, v]) => (
              <Row key={k} k={k} v={v} />
            ))}
          </dl>
        </section>
      ) : null}
    </div>
  );
}

function EstimateResultView({ payload }: { payload: unknown }) {
  if (payload === null || payload === undefined) {
    return null;
  }

  if (isPlainObject(payload) && payload.parseError === true && typeof payload.raw === "string") {
    return (
      <p className="mt-6 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-100">
        응답을 JSON으로 해석하지 못했습니다. (상태 {String(payload.status ?? "")})
      </p>
    );
  }

  if (!isPlainObject(payload)) {
    return (
      <p className="mt-6 text-sm text-zinc-700 dark:text-zinc-300">{String(payload)}</p>
    );
  }

  const data = payload.data;

  return (
    <div className="mt-6 space-y-6">
      {isPlainObject(data) ? <DataBlock data={data} /> : null}

      {Object.keys(payload).some((k) => !["success", "message", "data"].includes(k)) ? (
        <section>
          <h3 className="mb-2 text-sm font-semibold text-zinc-800 dark:text-zinc-200">기타</h3>
          <dl className="rounded-lg border border-zinc-200 bg-zinc-50/80 p-3 dark:border-zinc-700 dark:bg-zinc-900/40">
            {Object.entries(payload)
              .filter(([k]) => !["success", "message", "data"].includes(k))
              .map(([k, v]) =>
                v !== null && typeof v === "object" && !Array.isArray(v) ? (
                  <div key={k} className="py-2">
                    <p className="mb-1 text-sm font-medium text-zinc-600 dark:text-zinc-400">
                      {labelForKey(k)}
                    </p>
                    <pre className="overflow-auto rounded bg-zinc-100 p-2 text-xs dark:bg-zinc-900">
                      {JSON.stringify(v, null, 2)}
                    </pre>
                  </div>
                ) : (
                  <Row key={k} k={k} v={v} />
                )
              )}
          </dl>
        </section>
      ) : null}
    </div>
  );
}

export default function BookEstimatePage() {
  const params = useParams();
  const router = useRouter();
  const bookUid = typeof params.bookUid === "string" ? params.bookUid : "";

  const [ready, setReady] = useState(false);
  const [allowed, setAllowed] = useState(false);
  const [gateMessage, setGateMessage] = useState<string | null>(null);

  const [quantityInput, setQuantityInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [responseJson, setResponseJson] = useState<unknown>(null);

  useEffect(() => {
    const isLoggedIn = readLoggedIn();
    if (!isLoggedIn) {
      router.replace(`/login?next=${encodeURIComponent(`/book/${bookUid}/estimate`)}`);
      return;
    }
    if (!bookUid.trim()) {
      setGateMessage("북 UID가 없습니다.");
      setReady(true);
      return;
    }
    let cancelled = false;
    void fetchMyBookEntries().then(async (res) => {
      if (cancelled) return;
      if (res.status === 401) {
        router.replace(`/login?next=${encodeURIComponent(`/book/${bookUid}/estimate`)}`);
        return;
      }
      if (!res.ok) {
        setGateMessage("내 책 정보를 불러오지 못했습니다.");
        setAllowed(false);
        setReady(true);
        return;
      }
      try {
        const entries = (await res.json()) as MyBookEntry[];
        const uid = bookUid.trim();
        const entry = Array.isArray(entries) ? entries.find((e) => e.bookUid === uid) : undefined;
        if (!entry) {
          setGateMessage("이 책의 생성자만 견적 조회를 할 수 있습니다.");
          setAllowed(false);
        } else if (entry.finalized !== true) {
          setGateMessage("먼저 책 사진 목록에서 「편집 마치기」로 최종화해 주세요.");
          setAllowed(false);
        } else {
          setAllowed(true);
          setGateMessage(null);
        }
      } catch {
        setGateMessage("응답을 해석할 수 없습니다.");
        setAllowed(false);
      } finally {
        if (!cancelled) setReady(true);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [bookUid, router]);

  async function handleEstimate() {
    const uid = bookUid.trim();
    if (!uid) return;
    const raw = quantityInput.trim();
    const q = Number.parseInt(raw, 10);
    if (!Number.isFinite(q) || q < 1) {
      setError("수량은 1 이상의 정수로 입력하세요.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await postV1OrdersEstimate({
        items: [{ bookUid: uid, quantity: q }],
      });
      const text = await res.text();
      if (!text) {
        setResponseJson(null);
        if (!res.ok) {
          setError(`견적 조회 실패 (${res.status})`);
        }
        return;
      }
      try {
        setResponseJson(JSON.parse(text) as unknown);
      } catch {
        setResponseJson({ parseError: true, status: res.status, raw: text });
      }
    } catch {
      setError("네트워크 오류로 견적을 조회할 수 없습니다.");
      setResponseJson(null);
    } finally {
      setLoading(false);
    }
  }

  const backHref = `/book/${encodeURIComponent(bookUid.trim())}`;
  const estimatedPaidAmount = extractEstimatedPaidAmount(responseJson);
  const purchaseHref =
    Number.isFinite(Number.parseInt(quantityInput.trim(), 10)) &&
    Number.parseInt(quantityInput.trim(), 10) >= 1
      ? `/book/${encodeURIComponent(bookUid.trim())}/purchase?quantity=${encodeURIComponent(quantityInput.trim())}${
          estimatedPaidAmount !== null
            ? `&estimateTotal=${encodeURIComponent(String(estimatedPaidAmount))}`
            : ""
        }`
      : `/book/${encodeURIComponent(bookUid.trim())}/purchase`;

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-2xl px-4 py-8">
        <div className="mb-4 text-sm">
          <Link href={backHref} className="hover:underline">
            ← 책 사진 목록
          </Link>
        </div>
        <h1 className="mb-2 text-xl font-semibold">견적 조회</h1>
        <p className="mb-6 font-mono text-xs text-zinc-500 dark:text-zinc-400">{bookUid || "—"}</p>

        {!ready ? (
          <p className="text-sm text-zinc-500">확인 중…</p>
        ) : !allowed ? (
          <p className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-100">
            {gateMessage ?? "이용할 수 없습니다."}
          </p>
        ) : (
          <>
            <label className="mb-4 flex flex-col gap-1 text-sm text-zinc-700 dark:text-zinc-300">
              수량
              <input
                type="number"
                min={1}
                step={1}
                value={quantityInput}
                onChange={(e) => setQuantityInput(e.target.value)}
                placeholder="예: 1"
                disabled={loading}
                className="max-w-xs rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
              />
            </label>
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                disabled={loading}
                onClick={() => void handleEstimate()}
                className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
              >
                {loading ? "조회 중…" : "견적 조회"}
              </button>
              {responseJson !== null ? (
                <Link
                  href={purchaseHref}
                  className="rounded-md border border-violet-700 bg-violet-700 px-4 py-2 text-sm font-medium text-white hover:bg-violet-800 dark:border-violet-600 dark:bg-violet-800 dark:hover:bg-violet-700"
                >
                  구매하기
                </Link>
              ) : (
                <button
                  type="button"
                  disabled
                  className="rounded-md border border-zinc-300 bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-500 disabled:opacity-100 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-400"
                >
                  구매하기
                </button>
              )}
            </div>
            {error ? (
              <p className="mt-4 text-sm text-red-700 dark:text-red-300">{error}</p>
            ) : null}
            {responseJson !== null ? <EstimateResultView payload={responseJson} /> : null}
          </>
        )}
      </main>
    </div>
  );
}
