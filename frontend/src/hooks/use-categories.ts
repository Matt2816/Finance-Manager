import useSWR from "swr";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch, authenticatedJson } from "@/lib/authenticated-fetch";

export interface Category {
  id: number;
  slug: string;
  displayName: string;
}

export interface UncategorizedTransaction {
  id: number;
  name: string;
  merchant: string;
  amount: string;
  transactionDate: string;
  cardType: string;
  merchantRaw: string;
  merchantKey: string;
}

export interface MerchantRule {
  id: number;
  pattern: string;
  categoryId: number;
  categoryName: string;
  priority: number;
}

async function fetcher<T>(url: string): Promise<T> {
  return authenticatedJson<T>(url);
}

export function useCategories() {
  const { data, error, isLoading, mutate } = useSWR<Category[]>(
    `${getApiBaseUrl()}/api/categories`,
    fetcher
  );
  return { categories: data, error, isLoading, mutate };
}

export function useUncategorizedTransactions() {
  const { data, error, isLoading, mutate } = useSWR<UncategorizedTransaction[]>(
    `${getApiBaseUrl()}/api/categories/uncategorized`,
    fetcher
  );
  return { uncategorized: data, error, isLoading, mutate };
}

export function useMerchantRules() {
  const { data, error, isLoading, mutate } = useSWR<MerchantRule[]>(
    `${getApiBaseUrl()}/api/categories/rules`,
    fetcher
  );
  return { rules: data, error, isLoading, mutate };
}

export async function assignCategory(
  transactionId: number,
  categoryId: number,
  createRule = true
) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/categories/assign`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ transactionId, categoryId, createRule }),
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function deleteRule(ruleId: number) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/categories/rules/${ruleId}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function deleteCategory(categoryId: number) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/categories/${categoryId}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function createCategory(slug: string, displayName: string) {
  const res = await authenticatedFetch(`${getApiBaseUrl()}/api/categories`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ slug, displayName }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json() as Promise<Category>;
}
