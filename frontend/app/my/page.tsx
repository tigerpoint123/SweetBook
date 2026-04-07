"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { BookCoverTile } from "@/components/BookCoverTile";
import { fetchMyMemberInfo } from "@/lib/member-api";
import {
  chargeCreditSandbox,
  type CreditChargeApiResponse,
  fetchCredits,
  fetchSandboxCreditTransactions,
  type CreditsApiEnvelope,
  type CreditsData,
  type CreditTransaction,
  type CreditTransactionsEnvelope,
} from "@/lib/credit-api";
import {
  fetchV1Orders,
  type OrderListItem,
  type OrdersListEnvelope,
} from "@/lib/order-api";
import {
  buildBookCoverRows,
  deleteSweetbookBook,
  fetchBooksList,
  fetchMyBookEntries,
  mergeMyBooksWithSweetbookList,
  type BooksListEnvelope,
  type DeleteBookResponse,
  type MyBookEntry,
} from "@/lib/sweetbook-api";

type MemberInfo = {
  username: string;
};

function orderStatusIsCancelOrRefund(display: string | undefined): boolean {
  const t = display ?? "";
  return t.includes("취소") || t.includes("환불");
}

export default function MyPage() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [books, setBooks] = useState<unknown>(null);
  const [error, setError] = useState<string | null>(null);

  const [bookDeleteMode, setBookDeleteMode] = useState(false);
  const [selectedBookUids, setSelectedBookUids] = useState<Set<string>>(() => new Set());
  const [bookDeletePending, setBookDeletePending] = useState(false);
  const [bookDeleteMessage, setBookDeleteMessage] = useState<string | null>(null);

  const [credit, setCredit] = useState<CreditsData | null>(null);
  const [creditLoading, setCreditLoading] = useState(false);
  const [creditLoadError, setCreditLoadError] = useState<string | null>(null);
  const [transactions, setTransactions] = useState<CreditTransaction[]>([]);
  const [txLoadError, setTxLoadError] = useState<string | null>(null);
  const [shopOrders, setShopOrders] = useState<OrderListItem[]>([]);
  const [ordersLoadError, setOrdersLoadError] = useState<string | null>(null);
  const [ordersListMeta, setOrdersListMeta] = useState<{
    total: number;
    hasNext: boolean;
  } | null>(null);
  const [chargeFormOpen, setChargeFormOpen] = useState(false);
  const [chargeAmount, setChargeAmount] = useState("");
  const [chargeMemo, setChargeMemo] = useState("");
  const [chargePending, setChargePending] = useState(false);
  const [chargeMessage, setChargeMessage] = useState<string | null>(null);

  const bookCoverRows = useMemo(
    () => (books == null ? null : buildBookCoverRows(books, [])),
    [books]
  );

  const reloadMergedBooks = useCallback(async () => {
    const myRes = await fetchMyBookEntries();
    if (myRes.status === 401) {
      router.replace("/login");
      return;
    }
    if (!myRes.ok) {
      const t = await myRes.text();
      setError(t || `내 책 목록 조회 실패 (${myRes.status})`);
      setBooks({ success: true, data: { books: [] } });
      return;
    }
    const myEntries = (await myRes.json()) as MyBookEntry[];

    const booksRes = await fetchBooksList({ limit: 100, offset: 0 });
    let envelope: BooksListEnvelope | null = null;
    if (booksRes.ok) {
      try {
        envelope = (await booksRes.json()) as BooksListEnvelope;
      } catch {
        setError("책 목록 응답을 해석할 수 없습니다.");
      }
    } else {
      setError(`Sweetbook 책 목록 조회 실패 (${booksRes.status})`);
    }
    const merged = mergeMyBooksWithSweetbookList(myEntries, envelope);
    setBooks({ success: true, data: { books: merged } });
  }, [router]);

  const loadCreditPanel = useCallback(async () => {
    setCreditLoading(true);
    setCreditLoadError(null);
    setTxLoadError(null);
    setOrdersLoadError(null);
    try {
      const [cRes, tRes, oRes] = await Promise.all([
        fetchCredits(),
        fetchSandboxCreditTransactions(),
        fetchV1Orders(20, 0),
      ]);
      const cText = await cRes.text();
      const tText = await tRes.text();
      const oText = await oRes.text();
      if (!cRes.ok) {
        setCredit(null);
        setCreditLoadError(cText || `잔액 조회 실패 (${cRes.status})`);
      } else {
        try {
          const j = JSON.parse(cText) as CreditsApiEnvelope;
          if (j.success && j.data) {
            setCredit(j.data);
          } else {
            setCreditLoadError(j.message || "잔액 응답 형식이 올바르지 않습니다.");
          }
        } catch {
          setCreditLoadError("잔액 응답을 해석할 수 없습니다.");
        }
      }
      if (!tRes.ok) {
        setTransactions([]);
        setTxLoadError(tText || `거래 내역 조회 실패 (${tRes.status})`);
      } else {
        try {
          const j = JSON.parse(tText) as CreditTransactionsEnvelope;
          setTransactions(Array.isArray(j.data?.transactions) ? j.data.transactions : []);
        } catch {
          setTransactions([]);
          setTxLoadError("거래 내역 응답을 해석할 수 없습니다.");
        }
      }
      if (!oRes.ok) {
        setShopOrders([]);
        setOrdersListMeta(null);
        if (oRes.status === 401) {
          setOrdersLoadError("책 구매 내역을 보려면 로그인이 필요합니다.");
        } else {
          setOrdersLoadError(oText || `책 구매 내역 조회 실패 (${oRes.status})`);
        }
      } else {
        try {
          const j = JSON.parse(oText) as OrdersListEnvelope;
          const d = j.data;
          if (j.success && d) {
            setShopOrders(Array.isArray(d.items) ? d.items : []);
            setOrdersListMeta({
              total: typeof d.total === "number" ? d.total : 0,
              hasNext: d.hasNext === true,
            });
          } else {
            setShopOrders([]);
            setOrdersListMeta(null);
            setOrdersLoadError(j.message || "책 구매 내역 형식이 올바르지 않습니다.");
          }
        } catch {
          setShopOrders([]);
          setOrdersListMeta(null);
          setOrdersLoadError("책 구매 내역 응답을 해석할 수 없습니다.");
        }
      }
    } catch {
      setCreditLoadError("크래딧 정보를 불러오지 못했습니다.");
    } finally {
      setCreditLoading(false);
    }
  }, []);

  useEffect(() => {
    async function load() {
      setError(null);

      const meRes = await fetchMyMemberInfo();
      if (meRes.status === 401) {
        router.replace("/login");
        return;
      }
      if (!meRes.ok) {
        setError(`내 정보 조회 실패 (${meRes.status})`);
        return;
      }

      const me = (await meRes.json()) as MemberInfo;
      setUsername(me.username);

      await reloadMergedBooks();
      await loadCreditPanel();
    }

    void load().catch(() => setError("서버에 연결할 수 없습니다."));
  }, [router, reloadMergedBooks, loadCreditPanel]);

  function toggleBookDeleteMode() {
    setBookDeleteMessage(null);
    setBookDeleteMode((m) => {
      const next = !m;
      if (!next) setSelectedBookUids(new Set());
      return next;
    });
  }

  function toggleBookUidSelected(uid: string) {
    setSelectedBookUids((prev) => {
      const next = new Set(prev);
      if (next.has(uid)) next.delete(uid);
      else next.add(uid);
      return next;
    });
  }

  async function handleBookDeleteComplete() {
    setBookDeleteMessage(null);
    if (selectedBookUids.size === 0) {
      setBookDeleteMessage("삭제할 책을 선택하세요.");
      return;
    }
    setBookDeletePending(true);
    try {
      const uids = Array.from(selectedBookUids);
      let fail = 0;
      let lastMsg = "";
      for (const uid of uids) {
        const res = await deleteSweetbookBook(uid);
        const text = await res.text();
        if (!res.ok) {
          fail++;
          try {
            const j = JSON.parse(text) as { message?: string };
            if (j.message) lastMsg = j.message;
          } catch {
            /* keep lastMsg */
          }
          continue;
        }
        try {
          const j = JSON.parse(text) as DeleteBookResponse;
          if (j.message) lastMsg = j.message;
        } catch {
          /* keep */
        }
      }
      await reloadMergedBooks();
      setBookDeleteMode(false);
      setSelectedBookUids(new Set());
      if (fail > 0) {
        setBookDeleteMessage(
          fail === uids.length
            ? lastMsg || `${fail}권 삭제에 실패했습니다.`
            : `${uids.length - fail}권 삭제됨. ${fail}권 실패${lastMsg ? `: ${lastMsg}` : ""}`
        );
      } else {
        setBookDeleteMessage(lastMsg || "선택한 책을 삭제했습니다.");
      }
    } catch {
      setBookDeleteMessage("네트워크 오류로 삭제할 수 없습니다.");
    } finally {
      setBookDeletePending(false);
    }
  }

  async function handleChargeSubmit(e: React.FormEvent) {
    e.preventDefault();
    setChargeMessage(null);
    const raw = chargeAmount.trim();
    const amount = Number.parseInt(raw, 10);
    if (!Number.isFinite(amount) || amount < 1) {
      setChargeMessage("충전 금액은 1원 이상의 정수로 입력하세요.");
      return;
    }
    setChargePending(true);
    try {
      const res = await chargeCreditSandbox(amount, chargeMemo.trim());
      const text = await res.text();
      if (!res.ok) {
        let detail = text || `충전 실패 (${res.status})`;
        try {
          const j = JSON.parse(text) as { message?: string };
          if (j.message) detail = j.message;
        } catch {
          /* keep */
        }
        setChargeMessage(detail);
        return;
      }
      try {
        const j = JSON.parse(text) as CreditChargeApiResponse;
        if (j.success) {
          setChargeMessage(j.message || "충전이 반영되었습니다.");
        }
      } catch {
        setChargeMessage("충전이 반영되었습니다.");
      }
      setChargeAmount("");
      setChargeMemo("");
      setChargeFormOpen(false);
      await loadCreditPanel();
    } catch {
      setChargeMessage("네트워크 오류로 충전할 수 없습니다.");
    } finally {
      setChargePending(false);
    }
  }

  return (
    <div className="min-h-screen">
      <main className="mx-auto max-w-2xl px-4 py-10">
        <h1 className="mb-6 text-2xl font-semibold">내 정보</h1>

        {error ? (
          <p className="mb-6 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
            {error}
          </p>
        ) : null}

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-2 text-lg font-semibold">사용자</h2>
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            username: {username ?? "로딩 중..."}
          </p>
        </section>

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-3 text-lg font-semibold">충전금</h2>
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-zinc-600 dark:text-zinc-400">충전된 금액입니다.</p>
            <button
              type="button"
              onClick={() => {
                setChargeMessage(null);
                setChargeFormOpen((o) => !o);
              }}
              className="shrink-0 rounded-md border border-zinc-300 bg-white px-3 py-1.5 text-sm font-medium text-zinc-900 hover:bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-950 dark:text-zinc-100 dark:hover:bg-zinc-900"
            >
              {chargeFormOpen ? "닫기" : "충전"}
            </button>
          </div>
          {chargeFormOpen ? (
            <form
              onSubmit={(e) => void handleChargeSubmit(e)}
              className="mb-4 space-y-3 rounded-lg border border-zinc-200 bg-zinc-50 p-3 dark:border-zinc-700 dark:bg-zinc-900/40"
            >
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-zinc-700 dark:text-zinc-300">충전 금액 (원)</span>
                <input
                  type="number"
                  min={1}
                  step={1}
                  value={chargeAmount}
                  onChange={(e) => setChargeAmount(e.target.value)}
                  placeholder="예: 10000"
                  className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-zinc-700 dark:text-zinc-300">메모</span>
                <input
                  type="text"
                  value={chargeMemo}
                  onChange={(e) => setChargeMemo(e.target.value)}
                  placeholder="선택"
                  className="rounded-md border border-zinc-300 bg-white px-3 py-2 dark:border-zinc-600 dark:bg-zinc-950"
                />
              </label>
              <div className="flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={chargePending}
                  className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
                >
                  {chargePending ? "처리 중…" : "충전하기"}
                </button>
              </div>
            </form>
          ) : null}
          {chargeMessage ? (
            <p
              className={`mb-3 text-sm ${
                chargeMessage.includes("반영")
                  ? "text-emerald-700 dark:text-emerald-400"
                  : "text-red-700 dark:text-red-400"
              }`}
            >
              {chargeMessage}
            </p>
          ) : null}
          {creditLoadError ? (
            <p className="mb-3 rounded-md border border-red-200 bg-red-50 p-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              {creditLoadError}
            </p>
          ) : null}
          <div className="rounded-lg border border-zinc-200 bg-zinc-50 p-4 dark:border-zinc-700 dark:bg-zinc-900/40">
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
              현재 충전 잔액
            </p>
            <p className="mt-2 text-3xl font-semibold tabular-nums text-zinc-900 dark:text-zinc-100">
              {creditLoading ? (
                "…"
              ) : credit ? (
                <>
                  {credit.balance.toLocaleString("ko-KR")}
                  <span className="ml-1 text-xl font-medium text-zinc-600 dark:text-zinc-400">
                    {credit.currency === "KRW" ? "원" : credit.currency}
                  </span>
                </>
              ) : (
                <>
                  —
                  <span className="ml-1 text-xl font-medium text-zinc-600 dark:text-zinc-400">원</span>
                </>
              )}
            </p>
            {credit && !creditLoading ? (
              <p className="mt-2 text-xs text-zinc-500 dark:text-zinc-500">
                계정 {credit.accountUid} · {credit.env}
              </p>
            ) : null}
          </div>
        </section>

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <h2 className="mb-2 text-lg font-semibold">거래 내역</h2>
          <p className="mb-4 text-sm text-zinc-600 dark:text-zinc-400">
            크래딧 샌드박스 충전 기록과 책 구매 주문(GET /v1/orders)을 함께 표시합니다.
          </p>
          {creditLoading ? (
            <p className="text-sm text-zinc-500">불러오는 중…</p>
          ) : (
            <>
              <h3 className="mb-2 text-sm font-semibold text-zinc-800 dark:text-zinc-200">
                크래딧 거래 내역
              </h3>
              <p className="mb-2 text-xs text-zinc-500 dark:text-zinc-500">
                GET /v1/credits/transactions — 스윗북 거래 내역 조회
              </p>
              {txLoadError ? (
                <p className="mb-3 rounded-md border border-amber-200 bg-amber-50 p-2 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
                  {txLoadError}
                </p>
              ) : null}
              {transactions.length === 0 ? (
                <p className="mb-6 text-sm text-zinc-500">표시할 크래딧 충전 내역이 없습니다.</p>
              ) : (
                <div className="mb-6 overflow-x-auto rounded-lg border border-zinc-200 dark:border-zinc-700">
                  <table className="w-full min-w-[280px] text-left text-sm">
                    <thead className="border-b border-zinc-200 bg-zinc-50 text-xs text-zinc-600 dark:border-zinc-700 dark:bg-zinc-900/60 dark:text-zinc-400">
                      <tr>
                        <th className="px-3 py-2 font-medium">일시</th>
                        <th className="px-3 py-2 font-medium">금액</th>
                        <th className="px-3 py-2 font-medium">메모</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.map((row, i) => (
                        <tr
                          key={`${row.transactionId ?? "tx"}-${i}`}
                          className="border-b border-zinc-100 dark:border-zinc-800"
                        >
                          <td className="px-3 py-2 tabular-nums text-zinc-700 dark:text-zinc-300">
                            {new Date(row.createdAt).toLocaleString("ko-KR")}
                          </td>
                          <td className="px-3 py-2 font-medium tabular-nums text-zinc-900 dark:text-zinc-100">
                            {row.direction === "debit" ? "-" : "+"}
                            {row.amount.toLocaleString("ko-KR")}원
                          </td>
                          <td className="max-w-[12rem] truncate px-3 py-2 text-zinc-600 dark:text-zinc-400">
                            {row.memo || row.reasonDisplay || "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <h3 className="mb-2 text-sm font-semibold text-zinc-800 dark:text-zinc-200">
                책 구매
              </h3>
              <p className="mb-2 text-xs text-zinc-500 dark:text-zinc-500">
                GET /v1/orders — 스윗북 주문 목록(주문 UID, 상태, 결제 금액, 주문 시각)
                {ordersListMeta != null ? (
                  <span className="ml-1">
                    · 전체 {ordersListMeta.total}건
                    {ordersListMeta.hasNext ? " (다음 페이지 있음)" : ""}
                  </span>
                ) : null}
                <span className="ml-1">· 행 클릭 시 주문 상세 이동</span>
              </p>
              {ordersLoadError ? (
                <p className="mb-3 rounded-md border border-amber-200 bg-amber-50 p-2 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
                  {ordersLoadError}
                </p>
              ) : null}
              {shopOrders.length === 0 && !ordersLoadError ? (
                <p className="text-sm text-zinc-500">표시할 책 구매 내역이 없습니다.</p>
              ) : shopOrders.length > 0 ? (
                <div className="overflow-x-auto rounded-lg border border-zinc-200 dark:border-zinc-700">
                  <table className="w-full min-w-[320px] text-left text-sm">
                    <thead className="border-b border-zinc-200 bg-zinc-50 text-xs text-zinc-600 dark:border-zinc-700 dark:bg-zinc-900/60 dark:text-zinc-400">
                      <tr>
                        <th className="px-3 py-2 font-medium">주문</th>
                        <th className="px-3 py-2 font-medium">상태</th>
                        <th className="px-3 py-2 font-medium">금액</th>
                        <th className="px-3 py-2 font-medium">주문일시</th>
                      </tr>
                    </thead>
                    <tbody>
                      {shopOrders.map((row) => {
                        const statusAlert = orderStatusIsCancelOrRefund(row.orderStatusDisplay);
                        return (
                        <tr
                          key={row.orderUid}
                          onClick={() => router.push(`/my/orders/${encodeURIComponent(row.orderUid)}`)}
                          className="cursor-pointer border-b border-zinc-100 hover:bg-zinc-50 dark:border-zinc-800 dark:hover:bg-zinc-900/60"
                        >
                          <td className="px-3 py-2 font-mono text-xs text-zinc-800 dark:text-zinc-200">
                            {row.orderUid}
                          </td>
                          <td
                            className={
                              statusAlert
                                ? "px-3 py-2 text-red-600 dark:text-red-400"
                                : "px-3 py-2 text-zinc-700 dark:text-zinc-300"
                            }
                          >
                            <span
                              className={
                                statusAlert
                                  ? "text-xs text-red-500 dark:text-red-400"
                                  : "text-xs text-zinc-500"
                              }
                            >
                              {row.orderStatus}
                            </span>{" "}
                            {row.orderStatusDisplay}
                          </td>
                          <td className="px-3 py-2 tabular-nums font-medium text-zinc-900 dark:text-zinc-100">
                            {Number(row.totalAmount).toLocaleString("ko-KR", {
                              minimumFractionDigits: 0,
                              maximumFractionDigits: 2,
                            })}
                            원
                          </td>
                          <td className="px-3 py-2 tabular-nums text-zinc-700 dark:text-zinc-300">
                            {new Date(row.orderedAt).toLocaleString("ko-KR")}
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </>
          )}
        </section>

        <section className="mb-8 rounded-md border border-zinc-200 p-4 dark:border-zinc-800">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold">생성한 책</h2>
            {bookCoverRows && bookCoverRows.length > 0 ? (
              <>
                <button
                  type="button"
                  onClick={toggleBookDeleteMode}
                  className={`rounded-md border px-2.5 py-1 text-xs font-medium dark:border-zinc-600 ${
                    bookDeleteMode
                      ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                      : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50 dark:bg-zinc-950 dark:text-zinc-200 dark:hover:bg-zinc-900"
                  }`}
                >
                  {bookDeleteMode ? "삭제 취소" : "책 삭제"}
                </button>
                {bookDeleteMode ? (
                  <button
                    type="button"
                    disabled={bookDeletePending}
                    onClick={() => void handleBookDeleteComplete()}
                    className="rounded-md bg-red-700 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-800 disabled:opacity-50 dark:bg-red-800 dark:hover:bg-red-700"
                  >
                    {bookDeletePending ? "삭제 중…" : "선택 완료"}
                  </button>
                ) : null}
              </>
            ) : null}
          </div>
          {bookDeleteMessage ? (
            <p className="mb-3 text-xs text-zinc-600 dark:text-zinc-400">{bookDeleteMessage}</p>
          ) : null}
          {bookCoverRows === null ? (
            <p className="text-sm text-zinc-500">로딩 중…</p>
          ) : bookCoverRows.length === 0 ? (
            <p className="text-sm text-zinc-500">생성한 책이 없습니다.</p>
          ) : (
            <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
              {bookCoverRows.map((row) => (
                <li
                  key={row.key}
                  className="relative overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700"
                >
                  {bookDeleteMode && row.bookUid ? (
                    <div className="pointer-events-auto absolute left-1 top-1 z-10 rounded bg-white/90 p-0.5 shadow dark:bg-zinc-900/90">
                      <input
                        type="checkbox"
                        checked={selectedBookUids.has(row.bookUid)}
                        onChange={() => toggleBookUidSelected(row.bookUid!)}
                        aria-label={`삭제 선택: ${row.bookUid}`}
                        className="h-4 w-4 rounded border-zinc-300 text-zinc-900 focus:ring-2 focus:ring-zinc-400 dark:border-zinc-600 dark:bg-zinc-950 dark:focus:ring-zinc-500"
                      />
                    </div>
                  ) : null}
                  {bookDeleteMode && row.bookUid ? (
                    <button
                      type="button"
                      title="클릭하여 삭제 선택"
                      className="block w-full cursor-pointer rounded-lg p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                      onClick={() => toggleBookUidSelected(row.bookUid)}
                    >
                      <BookCoverTile url={row.imageUrl} />
                    </button>
                  ) : row.bookUid ? (
                    <Link
                      href={`/book/${encodeURIComponent(row.bookUid)}`}
                      className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
                    >
                      <BookCoverTile url={row.imageUrl} />
                    </Link>
                  ) : (
                    <BookCoverTile url={row.imageUrl} />
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}

