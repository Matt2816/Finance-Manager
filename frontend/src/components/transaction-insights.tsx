"use client";

import { useMemo } from "react";
import {
  MapPin,
  Store,
  CalendarDays,
  CreditCard,
  TrendingDown,
  TrendingUp,
  Receipt,
} from "lucide-react";

import { Transaction } from "@/types/transaction";
import { computeSpendingInsights } from "@/lib/transaction-analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cardTypes } from "@/components/data/data";

interface TransactionInsightsProps {
  transactions: Transaction[];
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-CA", {
    style: "currency",
    currency: "CAD",
    maximumFractionDigits: 0,
  }).format(value);
}

export function TransactionInsights({ transactions }: TransactionInsightsProps) {
  const insights = useMemo(
    () => computeSpendingInsights(transactions),
    [transactions]
  );

  const momTrend =
    insights.monthOverMonthChange === null
      ? null
      : insights.monthOverMonthChange >= 0
        ? "up"
        : "down";

  return (
    <Card className="col-span-full" data-screenshot="insights">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg sm:text-xl">Spending insights</CardTitle>
        <p className="text-sm text-muted-foreground">
          Patterns from {transactions.length} transactions
          {insights.transactionsWithLocation > 0 &&
            ` · ${insights.transactionsWithLocation} with location`}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <InsightTile
            icon={Store}
            label="Top merchant"
            value={insights.topMerchant?.name ?? "—"}
            detail={
              insights.topMerchant
                ? `${formatCurrency(insights.topMerchant.total)} · ${insights.topMerchant.count} visits`
                : undefined
            }
          />
          <InsightTile
            icon={MapPin}
            label="Top location"
            value={insights.topLocation?.label ?? "—"}
            detail={
              insights.topLocation
                ? `${formatCurrency(insights.topLocation.total)} · ${insights.topLocation.count} visits`
                : undefined
            }
          />
          <InsightTile
            icon={CalendarDays}
            label="Busiest day"
            value={
              insights.busiestDay
                ? new Date(insights.busiestDay.date).toLocaleDateString(
                    "en-CA",
                    { month: "short", day: "numeric" }
                  )
                : "—"
            }
            detail={
              insights.busiestDay
                ? `${formatCurrency(insights.busiestDay.total)} · ${insights.busiestDay.count} txns`
                : undefined
            }
          />
          <InsightTile
            icon={Receipt}
            label="Largest purchase"
            value={
              insights.largestTransaction
                ? formatCurrency(insights.largestTransaction.amount)
                : "—"
            }
            detail={insights.largestTransaction?.name}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="rounded-lg border p-3 sm:p-4">
            <div className="mb-3 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 text-sm font-medium">
                <CreditCard className="h-4 w-4 text-muted-foreground" />
                This month vs last
              </div>
              {momTrend && insights.monthOverMonthChange !== null && (
                <Badge
                  variant={
                    momTrend === "up" ? "destructive" : "secondary"
                  }
                  className="gap-1"
                >
                  {momTrend === "up" ? (
                    <TrendingUp className="h-3 w-3" />
                  ) : (
                    <TrendingDown className="h-3 w-3" />
                  )}
                  {Math.abs(insights.monthOverMonthChange).toFixed(0)}%
                </Badge>
              )}
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-muted-foreground">This month</p>
                <p className="text-xl font-semibold tabular-nums">
                  {formatCurrency(insights.thisMonthTotal)}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground">Last month</p>
                <p className="text-xl font-semibold tabular-nums">
                  {formatCurrency(insights.lastMonthTotal)}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border p-3 sm:p-4">
            <p className="mb-3 text-sm font-medium">Spend by card</p>
            <ul className="space-y-2">
              {insights.cardTypeBreakdown.map((item) => {
                const card = cardTypes.find((c) => c.value === item.cardType);
                const share =
                  insights.thisMonthTotal > 0
                    ? (item.total / insights.thisMonthTotal) * 100
                    : 0;
                return (
                  <li
                    key={item.cardType}
                    className="flex items-center justify-between gap-2 text-sm"
                  >
                    <span className="flex items-center gap-2 truncate">
                      {card?.icon && (
                        <card.icon className="h-5 w-5 shrink-0" />
                      )}
                      {card?.label ?? item.cardType}
                    </span>
                    <span className="shrink-0 tabular-nums text-muted-foreground">
                      {formatCurrency(item.total)}
                      {share > 0 && ` (${share.toFixed(0)}%)`}
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>

        {insights.cityBreakdown.length > 0 && (
          <div className="rounded-lg border p-3 sm:p-4">
            <p className="mb-3 text-sm font-medium">Top cities by spend</p>
            <div className="flex flex-wrap gap-2">
              {insights.cityBreakdown.map((city) => (
                <Badge key={city.city} variant="outline" className="text-xs">
                  {city.city}: {formatCurrency(city.total)} ({city.count})
                </Badge>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function InsightTile({
  icon: Icon,
  label,
  value,
  detail,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  detail?: string;
}) {
  return (
    <div className="rounded-lg border bg-muted/30 p-3">
      <div className="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className="h-3.5 w-3.5" />
        {label}
      </div>
      <p className="truncate font-semibold">{value}</p>
      {detail && (
        <p className="mt-0.5 truncate text-xs text-muted-foreground">{detail}</p>
      )}
    </div>
  );
}
