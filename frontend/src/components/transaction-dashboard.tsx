"use client";

import { useTransactions } from "@/hooks/use-transactions";
import { TransactionTable } from "@/components/transaction-table";
import { TransactionSummary } from "@/components/transaction-summary";
import { TransactionChart } from "@/components/transaction-chart";
import { TransactionComparisonChart } from "@/components/transaction-comparison-chart";
import { columns } from "@/components/columns";
export function TransactionDashboard() {
  const { transactions, isLoading, error } = useTransactions();

  if (isLoading) return <p>Loading transactions...</p>;
  if (error) return <p>Error loading transactions: {error.message}</p>;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <TransactionSummary transactions={transactions} />
      <TransactionComparisonChart transactions={transactions} />
      <TransactionChart transactions={transactions} />
      <TransactionTable columns={columns} data={transactions} />
    </div>
  );
}
