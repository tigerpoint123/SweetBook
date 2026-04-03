import { API_BASE } from "@/lib/api-config";

export const memberApi = {
  login: `${API_BASE}/api/member/login`,
  register: `${API_BASE}/api/member/member`,
  me: `${API_BASE}/api/member/member`,
} as const;

export async function postMemberForm(
  url: string,
  username: string,
  password: string
): Promise<Response> {
  const body = new URLSearchParams({ username, password });
  return fetch(url, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
}

export async function fetchMyMemberInfo(): Promise<Response> {
  return fetch(memberApi.me, {
    method: "GET",
    credentials: "include",
  });
}
