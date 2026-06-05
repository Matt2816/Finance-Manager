"use client";

import { PageHeader } from "@/components/dashboard/page-header";
import { SpendingCharts } from "@/components/spending-charts";
import { useForecasts, useCategorySpending } from "@/hooks/use-analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { DollarSign, TrendingUp, BarChart3 } from "lucide-react";

export default function ForecastsPage() {
  const { forecasts, isLoading: forecastsLoading } = useForecasts("TOTAL", 12);
  const { categorySpending, isLoading: categoryLoading } = useCategorySpending();

  const currentMonth = forecasts?.[0];
  const totalPredicted = forecasts?.reduce((sum, f) => sum + f.predictedAmount, 0) ?? 0;

  const categoryTotal = categorySpending?.reduce((sum, c) => sum + c.totalAmount, 0) ?? 1;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Forecasts"
        description="Spending predictions and category breakdowns."
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Next Month Forecast</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              ${currentMonth?.predictedAmount.toFixed(2) ?? "0.00"}
            </div>
            <p className="text-xs text-muted-foreground">
              Confidence: {((currentMonth?.confidence ?? 0) * 100).toFixed(0)}%
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">12-Month Projection</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              ${totalPredicted.toFixed(2)}
            </div>
            <p className="text-xs text-muted-foreground">
              Based on historical patterns
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Categories</CardTitle>
            <BarChart3 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {categorySpending?.length ?? 0}
            </div>
            <p className="text-xs text-muted-foreground">
              With tracked spending
            </p>
          </CardContent>
        </Card>
      </div>

      <SpendingCharts />

      <Card>
        <CardHeader>
          <CardTitle>Category Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          {categoryLoading ? (
            <div className="space-y-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="h-10 bg-muted rounded animate-pulse" />
              ))}
            </div>
          ) : categorySpending && categorySpending.length > 0 ? (
            <div className="space-y-4">
              {categorySpending
                .sort((a, b) => b.totalAmount - a.totalAmount)
                .map((cat) => {
                  const pct = Math.round((cat.totalAmount / categoryTotal) * 100);
                  return (
                    <div key={cat.categoryId} className="space-y-1.5">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium">{cat.categoryName}</span>
                        <span className="text-muted-foreground">
                          ${cat.totalAmount.toFixed(2)} ({pct}%)
                        </span>
                      </div>
                      <Progress value={pct} className="h-2" />
                      <p className="text-xs text-muted-foreground">
                        {cat.transactionCount} transactions
                      </p>
                    </div>
                  );
                })}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground text-center py-8">
              No category data available.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
