export const AUTH_STORAGE_KEY = "sweetbook_logged_in";

export function setLoggedIn(): void {
  sessionStorage.setItem(AUTH_STORAGE_KEY, "1");
}

export function clearLoggedIn(): void {
  sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

export function readLoggedIn(): boolean {
  if (typeof window === "undefined") return false;
  return sessionStorage.getItem(AUTH_STORAGE_KEY) === "1";
}
