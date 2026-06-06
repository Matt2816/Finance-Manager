"use client";

import { PageHeader } from "@/components/dashboard/page-header";
import { AnalyticsSummaryCards } from "@/components/analytics-summary-cards";
import { SpendingCharts } from "@/components/spending-charts";
import { MerchantLoyaltyMetrics } from "@/components/merchant-loyalty-metrics";
import { InsightCardsGrid } from "@/components/insight-cards-grid";
import { UncategorizedPrompt } from "@/components/uncategorized-prompt";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTransactions } from "@/hooks/use-transactions";
import { formatDateEDT } from "@/lib/date-utils";
import { ArrowUpRight, ArrowDownRight, Receipt } from "lucide-react";

function RecentTransactionsPreview() {
  const { transactions, isLoading } = useTransactions();

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Recent Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-10 bg-muted rounded animate-pulse" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  const recent = transactions
    ? [...transactions].sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()).slice(0, 6)
    : [];

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Receipt className="h-4 w-4 text-muted-foreground" />
          Recent Transactions
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-1">
          {recent.map((tx) => {
            const amount = parseFloat(tx.amount.replace("$", ""));
            const isPositive = amount > 0;
            return (
              <div
                key={tx.id}
                className="flex items-center justify-between py-2.5 px-3 rounded-lg hover:bg-muted/50 transition-colors"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium truncate">{tx.name || tx.merchant}</p>
                  <p className="text-xs text-muted-foreground">{formatDateEDT(tx.transactionDate)}</p>
                </div>
                <div className="flex items-center gap-1 text-sm font-medium shrink-0 ml-2">
                  {isPositive ? (
                    <ArrowUpRight className="h-3.5 w-3.5 text-red-500" />
                  ) : (
                    <ArrowDownRight className="h-3.5 w-3.5 text-green-500" />
                  )}
                  <span className={isPositive ? "text-red-600" : "text-green-600"}>
                    ${Math.abs(amount).toFixed(2)}
                  </span>
                </div>
              </div>
            );
          })}
          {recent.length === 0 && (
            <p className="text-sm text-muted-foreground text-center py-8">
              No transactions yet.
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export default function OverviewPage() {
  return (
    <div className="space-y-6 max-w-7xl">
      <UncategorizedPrompt />
      <PageHeader
        title="Overview"
        description="Your financial health at a glance."
      />

      <AnalyticsSummaryCards />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <SpendingCharts />
        </div>
        <RecentTransactionsPreview />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <h2 className="text-lg font-semibold mb-3">Latest Insights</h2>
          <InsightCardsGrid />
        </div>
        <div>
          <h2 className="text-lg font-semibold mb-3">Top Merchants</h2>
          <MerchantLoyaltyMetrics />
        </div>
      </div>
    </div>
  );
}
