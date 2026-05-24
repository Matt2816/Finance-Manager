import useSWR from "swr";

const fetcher = (url: string) => fetch(url).then((res) => res.json());

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
    "http://localhost:8080/api/insights",
    fetcher
  );
  return { insights: data, error, isLoading };
}

export function useTrends() {
  const { data, error, isLoading } = useSWR<Insight[]>(
    "http://localhost:8080/api/insights/trends",
    fetcher
  );
  return { trends: data, error, isLoading };
}

export function useForecasts(scope = "TOTAL", horizon?: number) {
  const url = new URL("http://localhost:8080/api/forecasts");
  url.searchParams.append("scope", scope);
  if (horizon) url.searchParams.append("horizon", horizon.toString());
  
  const { data, error, isLoading } = useSWR<Forecast[]>(url.toString(), fetcher);
  return { forecasts: data, error, isLoading };
}

export function useMerchantLoyalty(categoryId?: number, limit = 20) {
  const url = new URL("http://localhost:8080/api/merchant-loyalty");
  if (categoryId) url.searchParams.append("categoryId", categoryId.toString());
  url.searchParams.append("limit", limit.toString());
  
  const { data, error, isLoading } = useSWR<MerchantLoyalty[]>(url.toString(), fetcher, {
    revalidateOnFocus: false,
    shouldRetryOnError: false
  });
  return { loyalty: data, error, isLoading };
}

export function useCategorySpending() {
  const { data, error, isLoading } = useSWR<CategorySpending[]>(
    "http://localhost:8080/api/forecasts/category-spending",
    fetcher
  );
  return { categorySpending: data, error, isLoading };
}
