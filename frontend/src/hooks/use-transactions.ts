import useSWR from "swr";
import { Transaction } from "@/types/transaction";

async function fetcher(url: string): Promise<Transaction[]> {
  const res = await fetch(url);
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
    cardType: normalizeCardType(item.cardType),
    hash: item.hash || "needHash",
  }));
}

export function useTransactions() {
  const { data, error, isLoading } = useSWR<Transaction[]>(
    "/api/transactions",
    fetcher
  );

  const transactions = data ? cleanTransactions(data) : [];

  return {
    transactions,
    isLoading,
    error,
  };
}
