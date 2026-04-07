import { API_BASE } from "@/lib/api-config";

export type OrderShippingPayload = {
  recipientName: string;
  recipientPhone: string;
  postalCode: string;
  address1: string;
  address2?: string;
  memo?: string;
};

export type OrderItemPayload = {
  bookUid: string;
  quantity: number;
};

export type OrderEstimatePayload = {
  items: OrderItemPayload[];
};

/** POST /v1/orders/estimate — SESSION 쿠키 필요, Sweetbook 견적 API 프록시 */
export async function postV1OrdersEstimate(
  body: OrderEstimatePayload
): Promise<Response> {
  return fetch(`${API_BASE}/v1/orders/estimate`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

export type CreateOrderPayload = {
  items: OrderItemPayload[];
  shipping: OrderShippingPayload;
  externalRef?: string;
};

export type CreateOrderResponse = {
  success?: boolean;
  message?: string;
  data?: Record<string, unknown>;
  [key: string]: unknown;
};

/** POST /v1/orders — SESSION 쿠키 필요 */
export async function postV1Order(body: CreateOrderPayload): Promise<Response> {
  return fetch(`${API_BASE}/v1/orders`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

export type OrderListItem = {
  orderUid: string;
  orderStatus: number;
  orderStatusDisplay: string;
  totalAmount: number;
  orderedAt: string;
};

export type OrdersListData = {
  total: number;
  limit: number;
  offset: number;
  hasNext: boolean;
  items: OrderListItem[];
};

export type OrdersListEnvelope = {
  success?: boolean;
  message?: string;
  data?: OrdersListData;
};

export type OrderDetailItem = {
  itemUid?: string;
  bookUid?: string;
  bookTitle?: string;
  quantity?: number;
  unitPrice?: number;
  itemAmount?: number;
  itemStatus?: number;
  itemStatusDisplay?: string;
};

export type OrderDetailData = {
  orderUid?: string;
  orderType?: string;
  orderStatus?: number;
  orderStatusDisplay?: string;
  externalRef?: string | null;
  totalProductAmount?: number;
  totalShippingFee?: number;
  totalPackagingFee?: number;
  totalAmount?: number;
  paidCreditAmount?: number;
  recipientName?: string;
  recipientPhone?: string;
  postalCode?: string;
  address1?: string;
  address2?: string | null;
  orderedAt?: string;
  items?: OrderDetailItem[];
};

export type OrderDetailEnvelope = {
  success?: boolean;
  message?: string;
  data?: OrderDetailData;
};

/** GET /v1/orders?limit=&offset= — SESSION 쿠키 필요 */
export async function fetchV1Orders(
  limit = 20,
  offset = 0
): Promise<Response> {
  const sp = new URLSearchParams();
  sp.set("limit", String(limit));
  sp.set("offset", String(offset));
  return fetch(`${API_BASE}/v1/orders?${sp.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });
}

/** GET /v1/orders/{orderUid} — SESSION 쿠키 필요 */
export async function fetchV1OrderDetail(orderUid: string): Promise<Response> {
  return fetch(`${API_BASE}/v1/orders/${encodeURIComponent(orderUid)}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });
}

/** POST /v1/orders/{orderUid}/cancel — SESSION 쿠키 필요 */
export async function postV1OrderCancel(
  orderUid: string,
  reason: string
): Promise<Response> {
  return fetch(`${API_BASE}/v1/orders/${encodeURIComponent(orderUid)}/cancel`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason }),
  });
}

/** PATCH /v1/orders/{orderUid}/shipping — SESSION 쿠키 필요 */
export async function patchV1OrderShipping(
  orderUid: string,
  recipientName: string,
  address1: string
): Promise<Response> {
  return fetch(`${API_BASE}/v1/orders/${encodeURIComponent(orderUid)}/shipping`, {
    method: "PATCH",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ recipientName, address1 }),
  });
}
