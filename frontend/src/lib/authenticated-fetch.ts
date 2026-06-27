const TOKEN_KEY = "finance_manager_token";

export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string | null): void {
  if (typeof window === "undefined") return;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function authHeaders(token?: string | null): HeadersInit {
  const resolved = token ?? getStoredToken();
  if (!resolved) return {};
  return { Authorization: `Bearer ${resolved}` };
}

export class AuthError extends Error {
  constructor(message = "Unauthorized") {
    super(message);
    this.name = "AuthError";
  }
}

export async function authenticatedFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
  token?: string | null
): Promise<Response> {
  const headers = new Headers(init.headers);
  const auth = authHeaders(token);
  Object.entries(auth).forEach(([key, value]) => headers.set(key, value));

  const response = await fetch(input, { ...init, headers });

  const hadToken = !!(token ?? getStoredToken());
  if (response.status === 401 || (response.status === 403 && hadToken)) {
    setStoredToken(null);
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
    throw new AuthError();
  }

  return response;
}

export async function authenticatedJson<T>(
  input: RequestInfo | URL,
  init: RequestInit = {},
  token?: string | null
): Promise<T> {
  const response = await authenticatedFetch(input, init, token);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || response.statusText);
  }
  return response.json() as Promise<T>;
}
