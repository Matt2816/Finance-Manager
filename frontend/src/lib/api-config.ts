export function getApiBaseUrl(): string {
  if (process.env.NEXT_PUBLIC_API_URL) {
    return process.env.NEXT_PUBLIC_API_URL.replace(/\/$/, "");
  }
  if (typeof window === "undefined") {
    return "http://localhost:8080";
  }
  const hostname = window.location.hostname;
  const port = "8080";
  return `http://${hostname}:${port}`;
}

export function getAuthApiUrl(): string {
  return `${getApiBaseUrl()}/api/auth`;
}

export function getApiUrl(path: string): string {
  const base = getApiBaseUrl();
  const normalized = path.startsWith("/") ? path : `/${path}`;
  return `${base}${normalized.startsWith("/api") ? normalized : `/api${normalized}`}`;
}
