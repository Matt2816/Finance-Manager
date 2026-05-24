"use client";

import { useTransactions } from "@/hooks/use-transactions";
import { TransactionTable } from "@/components/transaction-table";
import { TransactionSummary } from "@/components/transaction-summary";
import { TransactionChart } from "@/components/transaction-chart";
import { TransactionComparisonChart } from "@/components/transaction-comparison-chart";
import { columns } from "@/components/columns";
import { AnalyticsSummaryCards } from "@/components/analytics-summary-cards";
import { SpendingCharts } from "@/components/spending-charts";
import { InsightCardsGrid } from "@/components/insight-cards-grid";
import { MerchantLoyaltyMetrics } from "@/components/merchant-loyalty-metrics";

export function TransactionDashboard() {
  const { transactions, isLoading, error } = useTransactions();

  if (isLoading) return <p>Loading transactions...</p>;
  if (error) return <p>Error loading transactions: {error.message}</p>;
  if (!transactions?.length) {
    return <p>No transactions yet. Add data via the backend API.</p>;
  }

  return (
    <div className="space-y-6">
      <AnalyticsSummaryCards />
      
      <SpendingCharts />
      
      <div>
        <h2 className="text-2xl font-bold mb-4">Insights</h2>
        <InsightCardsGrid />
      </div>
      
      <MerchantLoyaltyMetrics />
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <TransactionSummary transactions={transactions} />
        <TransactionComparisonChart transactions={transactions} />
        <TransactionChart transactions={transactions} />
        <TransactionTable columns={columns} data={transactions} />
      </div>
    </div>
  );
}
