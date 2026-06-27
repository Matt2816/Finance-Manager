"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useInsights, useForecasts, useIncomeSummary, useCategorySpending } from "@/hooks/use-analytics";
import { TrendingUp, TrendingDown, AlertCircle, DollarSign, ArrowDownLeft, ArrowUpRight, Wallet } from "lucide-react";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-CA", {
    style: "currency",
    currency: "CAD",
    maximumFractionDigits: 0,
  }).format(value);
}

export function AnalyticsSummaryCards() {
  const { insights, isLoading: insightsLoading } = useInsights();
  const { forecasts, isLoading: forecastsLoading } = useForecasts();
  const { summary, isLoading: summaryLoading } = useIncomeSummary();
  const { categorySpending, isLoading: categorySpendingLoading } = useCategorySpending();

  const now = new Date();
  const currentMonthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
  const prevMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const prevMonthStart = `${prevMonthDate.getFullYear()}-${String(prevMonthDate.getMonth() + 1).padStart(2, "0")}-01`;
  const prevMonthEndDate = new Date(now.getFullYear(), now.getMonth(), 0);
  const prevMonthEnd = `${prevMonthEndDate.getFullYear()}-${String(prevMonthEndDate.getMonth() + 1).padStart(2, "0")}-${String(prevMonthEndDate.getDate()).padStart(2, "0")}`;
  const { summary: prevSummary, isLoading: prevSummaryLoading } = useIncomeSummary(prevMonthStart, prevMonthEnd);

  if (insightsLoading || forecastsLoading || summaryLoading || categorySpendingLoading || prevSummaryLoading) {
    return <div>Loading summary...</div>;
  }

  const totalInsights = insights?.length || 0;
  const highSeverityInsights = insights?.filter(i => i.severity === "HIGH").length || 0;
  const currentMonthForecast = forecasts?.[0];
  const forecastAmount = currentMonthForecast?.predictedAmount || 0;

  const currentExpenses = summary?.totalExpenses ?? 0;
  const prevExpenses = prevSummary?.totalExpenses ?? 0;
  const spendingTrend = prevExpenses > 0 ? ((currentExpenses - prevExpenses) / prevExpenses) * 100 : 0;
  const trendSign = spendingTrend >= 0 ? "+" : "";

  const sortedCategories = [...(categorySpending ?? [])].sort((a, b) => b.totalAmount - a.totalAmount);
  const topCategory = sortedCategories[0];
  const totalCategorySpend = sortedCategories.reduce((sum, c) => sum + c.totalAmount, 0);
  const topCategoryPercent = totalCategorySpend > 0 ? ((topCategory?.totalAmount ?? 0) / totalCategorySpend) * 100 : 0;

  return (
    <div className="space-y-4">
      {/* Financial Health Row */}
      <div className="grid grid-cols-3 sm:grid-cols-3 gap-2 sm:gap-4">
        <Card>
          <CardContent className="p-3 sm:p-4 flex flex-col sm:flex-row items-center sm:items-center gap-1.5 sm:gap-3 text-center sm:text-left">
            <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-green-100 flex items-center justify-center shrink-0">
              <ArrowDownLeft className="h-4 w-4 sm:h-5 sm:w-5 text-green-600" />
            </div>
            <div className="min-w-0">
              <p className="text-[10px] sm:text-sm text-muted-foreground leading-tight">Income</p>
              <p className="text-sm sm:text-xl font-bold truncate">{formatCurrency(summary?.totalIncome ?? 0)}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-3 sm:p-4 flex flex-col sm:flex-row items-center sm:items-center gap-1.5 sm:gap-3 text-center sm:text-left">
            <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-red-100 flex items-center justify-center shrink-0">
              <ArrowUpRight className="h-4 w-4 sm:h-5 sm:w-5 text-red-600" />
            </div>
            <div className="min-w-0">
              <p className="text-[10px] sm:text-sm text-muted-foreground leading-tight">Expenses</p>
              <p className="text-sm sm:text-xl font-bold truncate">{formatCurrency(summary?.totalExpenses ?? 0)}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-3 sm:p-4 flex flex-col sm:flex-row items-center sm:items-center gap-1.5 sm:gap-3 text-center sm:text-left">
            <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-blue-100 flex items-center justify-center shrink-0">
              <Wallet className="h-4 w-4 sm:h-5 sm:w-5 text-blue-600" />
            </div>
            <div className="min-w-0">
              <p className="text-[10px] sm:text-sm text-muted-foreground leading-tight">Savings</p>
              <p className="text-sm sm:text-xl font-bold truncate">{formatCurrency(summary?.netSavings ?? 0)}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Existing Analytics Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Forecast</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">${forecastAmount.toFixed(2)}</div>
            <p className="text-xs text-muted-foreground">
              Based on historical patterns
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Insights</CardTitle>
            <AlertCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalInsights}</div>
            <p className="text-xs text-muted-foreground">
              {highSeverityInsights} high priority
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Spending Trend</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{trendSign}{spendingTrend.toFixed(1)}%</div>
            <p className="text-xs text-muted-foreground">
              vs last month
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Top Category</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{topCategory?.categoryName || "—"}</div>
            <p className="text-xs text-muted-foreground">
              {topCategoryPercent.toFixed(0)}% of total spend
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
