"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useInsights, useForecasts } from "@/hooks/use-analytics";
import { TrendingUp, TrendingDown, AlertCircle, DollarSign } from "lucide-react";

export function AnalyticsSummaryCards() {
  const { insights, isLoading: insightsLoading } = useInsights();
  const { forecasts, isLoading: forecastsLoading } = useForecasts();

  if (insightsLoading || forecastsLoading) {
    return <div>Loading summary...</div>;
  }

  const totalInsights = insights?.length || 0;
  const highSeverityInsights = insights?.filter(i => i.severity === "HIGH").length || 0;
  const currentMonthForecast = forecasts?.[0];
  const forecastAmount = currentMonthForecast?.predictedAmount || 0;

  return (
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
          <div className="text-2xl font-bold">+12.5%</div>
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
          <div className="text-2xl font-bold">Groceries</div>
          <p className="text-xs text-muted-foreground">
            35% of total spend
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
