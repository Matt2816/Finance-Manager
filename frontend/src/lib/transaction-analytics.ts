import { Transaction } from "@/types/transaction";
import { extractCityFromAddress } from "@/lib/location";

export function parseAmount(amount: string): number {
  const cleaned = amount.replace(/[^0-9.-]/g, "");
  const value = Number.parseFloat(cleaned);
  return Number.isFinite(value) ? value : 0;
}

export function getMagnitude(amount: number): "low" | "medium" | "high" {
  if (amount > 100) return "high";
  if (amount > 20) return "medium";
  return "low";
}

export type SpendingInsight = {
  topMerchant: { name: string; total: number; count: number } | null;
  topLocation: { label: string; total: number; count: number } | null;
  busiestDay: { date: string; total: number; count: number } | null;
  largestTransaction: { name: string; amount: number; date: string } | null;
  cardTypeBreakdown: { cardType: string; total: number; count: number }[];
  cityBreakdown: { city: string; total: number; count: number }[];
  thisMonthTotal: number;
  lastMonthTotal: number;
  monthOverMonthChange: number | null;
  averageTransaction: number;
  transactionsWithLocation: number;
};

function monthKey(date: Date): string {
  return `${date.getFullYear()}-${date.getMonth()}`;
}

function aggregateByKey<T extends string>(
  items: { key: T; amount: number }[]
): Map<T, { total: number; count: number }> {
  const map = new Map<T, { total: number; count: number }>();
  for (const { key, amount } of items) {
    const existing = map.get(key) ?? { total: 0, count: 0 };
    map.set(key, {
      total: existing.total + amount,
      count: existing.count + 1,
    });
  }
  return map;
}

function topEntry<T extends string>(
  map: Map<T, { total: number; count: number }>
): { key: T; total: number; count: number } | null {
  let best: { key: T; total: number; count: number } | null = null;
  for (const [key, stats] of map) {
    if (!best || stats.total > best.total) {
      best = { key, ...stats };
    }
  }
  return best;
}

export function computeSpendingInsights(
  transactions: Transaction[]
): SpendingInsight {
  const now = new Date();
  const thisMonth = monthKey(now);
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const lastMonth = monthKey(lastMonthDate);

  let thisMonthTotal = 0;
  let lastMonthTotal = 0;
  let totalSpend = 0;
  let largestTransaction: SpendingInsight["largestTransaction"] = null;

  const merchantItems: { key: string; amount: number }[] = [];
  const locationItems: { key: string; amount: number }[] = [];
  const dayItems: { key: string; amount: number }[] = [];
  const cardItems: { key: string; amount: number }[] = [];
  const cityItems: { key: string; amount: number }[] = [];
  let transactionsWithLocation = 0;

  for (const tx of transactions) {
    const amount = parseAmount(tx.amount);
    totalSpend += amount;

    const txDate = new Date(tx.transactionDate);
    const txMonth = monthKey(txDate);
    if (txMonth === thisMonth) thisMonthTotal += amount;
    if (txMonth === lastMonth) lastMonthTotal += amount;

    const merchant = tx.merchant?.trim() || tx.name?.trim() || "Unknown";
    merchantItems.push({ key: merchant, amount });

    const day = tx.transactionDate.split("T")[0];
    dayItems.push({ key: day, amount });

    cardItems.push({ key: tx.cardType, amount });

    const address = tx.address?.trim();
    if (address) {
      transactionsWithLocation += 1;
      locationItems.push({ key: address, amount });
      cityItems.push({ key: extractCityFromAddress(address), amount });
    }

    if (!largestTransaction || amount > largestTransaction.amount) {
      largestTransaction = {
        name: tx.name,
        amount,
        date: day,
      };
    }
  }

  const topMerchantEntry = topEntry(aggregateByKey(merchantItems));
  const topLocationEntry = topEntry(aggregateByKey(locationItems));
  const busiestDayEntry = topEntry(aggregateByKey(dayItems));

  const cardTypeBreakdown = [...aggregateByKey(cardItems).entries()]
    .map(([cardType, stats]) => ({ cardType, ...stats }))
    .sort((a, b) => b.total - a.total);

  const cityBreakdown = [...aggregateByKey(cityItems).entries()]
    .map(([city, stats]) => ({ city, ...stats }))
    .sort((a, b) => b.total - a.total)
    .slice(0, 8);

  const monthOverMonthChange =
    lastMonthTotal > 0
      ? ((thisMonthTotal - lastMonthTotal) / lastMonthTotal) * 100
      : null;

  return {
    topMerchant: topMerchantEntry
      ? {
          name: topMerchantEntry.key,
          total: topMerchantEntry.total,
          count: topMerchantEntry.count,
        }
      : null,
    topLocation: topLocationEntry
      ? {
          label: formatLocationInsightLabel(topLocationEntry.key),
          total: topLocationEntry.total,
          count: topLocationEntry.count,
        }
      : null,
    busiestDay: busiestDayEntry
      ? {
          date: busiestDayEntry.key,
          total: busiestDayEntry.total,
          count: busiestDayEntry.count,
        }
      : null,
    largestTransaction,
    cardTypeBreakdown,
    cityBreakdown,
    thisMonthTotal,
    lastMonthTotal,
    monthOverMonthChange,
    averageTransaction:
      transactions.length > 0 ? totalSpend / transactions.length : 0,
    transactionsWithLocation,
  };
}

function formatLocationInsightLabel(address: string): string {
  const firstLine = address
    .split("\n")
    .map((line) => line.trim())
    .find(Boolean);
  return firstLine ?? address;
}

export type LocationSpendPoint = {
  address: string;
  label: string;
  lat: number;
  lon: number;
  totalSpend: number;
  transactionCount: number;
};

export function aggregateSpendByAddress(
  transactions: Transaction[]
): Map<string, { totalSpend: number; transactionCount: number }> {
  const map = new Map<string, { totalSpend: number; transactionCount: number }>();
  for (const tx of transactions) {
    const address = tx.address?.trim();
    if (!address) continue;
    const amount = parseAmount(tx.amount);
    const existing = map.get(address) ?? { totalSpend: 0, transactionCount: 0 };
    map.set(address, {
      totalSpend: existing.totalSpend + amount,
      transactionCount: existing.transactionCount + 1,
    });
  }
  return map;
}
