const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const memberApi = {
  login: `${API_BASE}/api/member/login`,
  register: `${API_BASE}/api/member/member`,
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
