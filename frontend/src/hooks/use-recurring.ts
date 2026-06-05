import useSWR from "swr";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch, authenticatedJson } from "@/lib/authenticated-fetch";

export interface RecurringTransaction {
  id: number;
  name: string;
  amount: string;
  monthlyEquivalent?: string;
  direction: "DEBIT" | "CREDIT";
  frequency: "WEEKLY" | "BIWEEKLY" | "MONTHLY";
  startDate: string;
  endDate: string | null;
  categoryId: number | null;
  cardType: string;
  active: boolean;
  lastGeneratedDate: string | null;
  createdAt: string;
}

async function fetcher<T>(url: string): Promise<T> {
  return authenticatedJson<T>(url);
}

export function useRecurringTransactions(direction?: "DEBIT" | "CREDIT", active?: boolean) {
  const params = new URLSearchParams();
  if (direction) params.append("direction", direction);
  if (active !== undefined) params.append("active", String(active));
  const query = params.toString() ? `?${params.toString()}` : "";
  const { data, error, isLoading, mutate } = useSWR<RecurringTransaction[]>(
    `${getApiBaseUrl()}/api/recurring${query}`,
    fetcher
  );
  return { recurring: data, error, isLoading, mutate };
}

export async function createRecurring(payload: {
  name: string;
  amount: string;
  direction: "DEBIT" | "CREDIT";
  frequency: "WEEKLY" | "BIWEEKLY" | "MONTHLY";
  startDate: string;
  endDate?: string | null;
  categoryId?: number | null;
  cardType?: string;
}) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/recurring`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json() as Promise<RecurringTransaction>;
}

export async function updateRecurring(
  id: number,
  payload: {
    name: string;
    amount: string;
    direction: "DEBIT" | "CREDIT";
    frequency: "WEEKLY" | "BIWEEKLY" | "MONTHLY";
    startDate: string;
    endDate?: string | null;
    categoryId?: number | null;
    cardType?: string;
  }
) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/recurring/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json() as Promise<RecurringTransaction>;
}

export async function pauseRecurring(id: number) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/recurring/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function resumeRecurring(id: number) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/recurring/${id}/resume`, {
    method: "POST",
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function generateNowRecurring(id: number) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/recurring/${id}/generate-now`, {
    method: "POST",
  });
  if (!res.ok) throw new Error(await res.text());
}
