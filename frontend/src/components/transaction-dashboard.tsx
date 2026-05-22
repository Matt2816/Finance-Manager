"use client";

import { useTransactions } from "@/hooks/use-transactions";
import { TransactionTable } from "@/components/transaction-table";
import { TransactionSummary } from "@/components/transaction-summary";
import { TransactionChart } from "@/components/transaction-chart";
import { TransactionComparisonChart } from "@/components/transaction-comparison-chart";
import { TransactionInsights } from "@/components/transaction-insights";
import { SpendingLocationHeatmap } from "@/components/spending-location-heatmap";
import { columns } from "@/components/columns";

export function TransactionDashboard() {
  const { transactions, isLoading, error } = useTransactions();

  if (isLoading) {
    return (
      <p className="py-12 text-center text-muted-foreground">
        Loading transactions…
      </p>
    );
  }
  if (error) {
    return (
      <p className="py-12 text-center text-destructive">
        Error loading transactions: {error.message}
      </p>
    );
  }
  if (!transactions?.length) {
    return (
      <p className="py-12 text-center text-muted-foreground">
        No transactions yet. Add data via the backend API.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4 sm:gap-6">
      <TransactionInsights transactions={transactions} />
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-6">
        <TransactionSummary transactions={transactions} />
        <TransactionComparisonChart transactions={transactions} />
        <TransactionChart transactions={transactions} />
      </div>
      <SpendingLocationHeatmap transactions={transactions} />
      <TransactionTable columns={columns} data={transactions} />
    </div>
  );
}
