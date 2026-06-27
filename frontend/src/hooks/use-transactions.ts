import { useMemo } from "react";
import useSWR from "swr";
import { Transaction } from "@/types/transaction";
import { authenticatedFetch } from "@/lib/authenticated-fetch";

async function fetcher(url: string): Promise<Transaction[]> {
  const res = await authenticatedFetch(url);
  const data = await res.json();

  if (!res.ok) {
    const message =
      typeof data === "object" && data !== null && "error" in data
        ? String((data as { error: string }).error)
        : res.statusText;
    throw new Error(message);
  }

  if (!Array.isArray(data)) {
    throw new Error("Invalid response: expected a list of transactions");
  }

  return data;
}

const CARD_TYPE_KEYWORDS = ["amex", "mastercard", "visa", "debit"] as const;

function normalizeCardType(cardType: string): Transaction["cardType"] {
  const match = CARD_TYPE_KEYWORDS.find((type) =>
    cardType.toLowerCase().includes(type)
  );
  return match ?? "other";
}

function cleanTransactions(data: Transaction[]): Transaction[] {
  return data.map((item) => ({
    ...item,
    label: "transaction" as const,
    cardType: normalizeCardType(item.cardType ?? ""),
    hash: item.hash || "needHash",
    address: item.address ?? "",
  }));
}

export function useTransactions() {
  const { data, error, isLoading, mutate } = useSWR<Transaction[]>(
    "/api/transactions",
    fetcher
  );

  const transactions = useMemo(() => data ? cleanTransactions(data) : [], [data]);

  return {
    transactions,
    isLoading,
    error,
    mutate,
  };
}
