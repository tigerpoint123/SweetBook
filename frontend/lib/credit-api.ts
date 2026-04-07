import { API_BASE } from "@/lib/api-config";

export type CreditsData = {
  accountUid: string;
  balance: number;
  currency: string;
  env: string;
  createdAt: string;
  updatedAt: string;
};

export type CreditsApiEnvelope = {
  success: boolean;
  message: string;
  data: CreditsData;
};

export type CreditTransaction = {
  transactionId: string;
  accountUid: string;
  reasonCode: number;
  reasonDisplay: string;
  direction: string;
  amount: number;
  balanceAfter: number;
  memo: string;
  isTest: boolean;
  createdAt: string;
};

export type CreditTransactionsEnvelope = {
  success: boolean;
  message: string;
  data: {
    transactions: CreditTransaction[];
    total: number;
    limit: number;
    offset: number;
  };
};

export type CreditChargeData = {
  transactionUid: string;
  amount: number;
  balanceAfter: number;
  currency: string;
};

export type CreditChargeApiResponse = {
  success: boolean;
  message: string;
  data: CreditChargeData;
};

export async function fetchCredits(): Promise<Response> {
  return fetch(`${API_BASE}/v1/credits`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });
}

export async function chargeCreditSandbox(
  amount: number,
  memo: string
): Promise<Response> {
  return fetch(`${API_BASE}/v1/credits/sandbox/charge`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount, memo }),
  });
}

export async function fetchSandboxCreditTransactions(): Promise<Response> {
  return fetch(`${API_BASE}/v1/credits/transactions`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });
}
