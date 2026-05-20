const DEFAULT_BACKEND_API_URL = "http://127.0.0.1:8080/api";

/** Base URL for the Spring Boot API (server-side only). */
export function getBackendApiUrl(): string {
  return (
    process.env.BACKEND_API_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    DEFAULT_BACKEND_API_URL
  ).replace(/\/$/, "");
}

export async function fetchBackend<T>(path: string): Promise<T> {
  const url = `${getBackendApiUrl()}${path.startsWith("/") ? path : `/${path}`}`;
  const response = await fetch(url);

  if (!response.ok) {
    const body = await response.text();
    throw new Error(
      `Backend request failed (${response.status}): ${body || response.statusText}`
    );
  }

  return response.json() as Promise<T>;
}
