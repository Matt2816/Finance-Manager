import useSWR from "swr";
import { Transaction } from "@/types/transaction";

const fetcher = (url: string) => fetch(url).then((res) => res.json());

export function useTransactions() {
  const { data, error, isLoading } = useSWR<Transaction[]>(
    "/api/transactions",
    fetcher
  );

  const cleanedData = data?.map((item) => ({
    ...item,
    label: "transaction",
    cardType:
      ["amex", "mastercard", "visa", "debit"].find((type) =>
        item.cardType.toLowerCase().includes(type)
      ) || "other",
    hash: item.hash ? item.hash : "needHash",
  }));

  return {
    transactions: cleanedData as Transaction[],
    isLoading,
    error,
  };
}
