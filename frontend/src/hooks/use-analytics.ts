import useSWR from "swr";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedJson } from "@/lib/authenticated-fetch";

async function fetcher<T>(url: string): Promise<T> {
  return authenticatedJson<T>(url);
}

export interface Insight {
  id: number;
  insightType: string;
  title: string;
  body: string;
  severity: string;
  payloadJson: string;
  validFrom: string;
  validTo: string;
  generatedAt: string;
  rankScore: number;
}

export interface Forecast {
  id: number;
  scope: string;
  scopeKey: string | null;
  forecastDate: string;
  predictedAmount: number;
  lowerBound: number;
  upperBound: number;
  method: string;
  confidence: number;
}

export interface MerchantLoyalty {
  id: number;
  merchantKey: string;
  canonicalName: string;
  categoryId: number | null;
  totalTransactions: number;
  totalSpend: number;
  avgTransactionSize: number;
  firstVisit: string;
  lastVisit: string;
  visitFrequencyDays: number | null;
  loyaltyScore: number;
  spendGrowthRate: number | null;
  calculatedAt: string;
}

export interface CategorySpending {
  categoryId: number;
  categoryName: string;
  totalAmount: number;
  transactionCount: number;
}

export function useInsights() {
  const { data, error, isLoading } = useSWR<Insight[]>(
    `${getApiBaseUrl()}/api/insights`,
    fetcher<Insight[]>
  );
  return { insights: data, error, isLoading };
}

export function useTrends() {
  const { data, error, isLoading } = useSWR<Insight[]>(
    `${getApiBaseUrl()}/api/insights/trends`,
    fetcher<Insight[]>
  );
  return { trends: data, error, isLoading };
}

export function useForecasts(scope = "TOTAL", horizon?: number) {
  const url = new URL(`${getApiBaseUrl()}/api/forecasts`);
  url.searchParams.append("scope", scope);
  if (horizon) url.searchParams.append("horizon", horizon.toString());
  
  const { data, error, isLoading } = useSWR<Forecast[]>(url.toString(), fetcher<Forecast[]>);
  return { forecasts: data, error, isLoading };
}

export function useMerchantLoyalty(categoryId?: number, limit = 20) {
  const url = new URL(`${getApiBaseUrl()}/api/merchant-loyalty`);
  if (categoryId) url.searchParams.append("categoryId", categoryId.toString());
  url.searchParams.append("limit", limit.toString());
  
  const { data, error, isLoading } = useSWR<MerchantLoyalty[]>(url.toString(), fetcher<MerchantLoyalty[]>, {
    revalidateOnFocus: false,
    shouldRetryOnError: false
  });
  return { loyalty: data, error, isLoading };
}

export function useCategorySpending(from?: string, to?: string) {
  const url = new URL(`${getApiBaseUrl()}/api/forecasts/category-spending`);
  if (from) url.searchParams.append("from", from);
  if (to) url.searchParams.append("to", to);

  const { data, error, isLoading } = useSWR<CategorySpending[]>(
    url.toString(),
    fetcher<CategorySpending[]>
  );
  return { categorySpending: data, error, isLoading };
}

export interface IncomeSummary {
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
}

export function useIncomeSummary(from?: string, to?: string) {
  const url = new URL(`${getApiBaseUrl()}/api/analytics/income-summary`);
  if (from) url.searchParams.append("from", from);
  if (to) url.searchParams.append("to", to);

  const { data, error, isLoading, mutate } = useSWR<IncomeSummary>(
    url.toString(),
    fetcher<IncomeSummary>
  );
  return { summary: data, error, isLoading, mutate };
}
