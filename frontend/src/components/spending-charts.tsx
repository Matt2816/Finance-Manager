"use client";

import { useState, useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCategorySpending, useForecasts } from "@/hooks/use-analytics";
import { Bar, BarChart, Line, LineChart, Pie, PieChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from "recharts";
import { Button } from "@/components/ui/button";

type Range = "1M" | "3M" | "6M" | "1Y" | "All";

const PIE_COLORS = [
  "#3b82f6", "#ef4444", "#22c55e", "#f59e0b", "#8b5cf6",
  "#ec4899", "#06b6d4", "#f97316", "#84cc16", "#6366f1",
  "#14b8a6", "#d946ef", "#eab308", "#4f46e5", "#0ea5e9",
];

const ranges: { value: Range; label: string }[] = [
  { value: "1M", label: "1 Month" },
  { value: "3M", label: "3 Months" },
  { value: "6M", label: "6 Months" },
  { value: "1Y", label: "1 Year" },
  { value: "All", label: "All Time" },
];

function getDateRange(range: Range) {
  const now = new Date();
  const to = now.toISOString().split("T")[0];
  let from = "";
  switch (range) {
    case "1M":
      from = new Date(now.getFullYear(), now.getMonth() - 1, now.getDate()).toISOString().split("T")[0];
      break;
    case "3M":
      from = new Date(now.getFullYear(), now.getMonth() - 3, now.getDate()).toISOString().split("T")[0];
      break;
    case "6M":
      from = new Date(now.getFullYear(), now.getMonth() - 6, now.getDate()).toISOString().split("T")[0];
      break;
    case "1Y":
      from = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate()).toISOString().split("T")[0];
      break;
    case "All":
      from = "";
      break;
  }
  return { from, to: range === "All" ? "" : to };
}

export function SpendingCharts() {
  const { forecasts, isLoading: forecastsLoading } = useForecasts("TOTAL", 12);
  const [range, setRange] = useState<Range>("1Y");
  const { from, to } = useMemo(() => getDateRange(range), [range]);
  const { categorySpending, isLoading: categoryLoading } = useCategorySpending(from || undefined, to || undefined);

  if (forecastsLoading || categoryLoading) {
    return <div>Loading charts...</div>;
  }

  const forecastData = forecasts?.map(f => ({
    date: new Date(f.forecastDate).toLocaleDateString("en-US", { month: "short" }),
    predicted: f.predictedAmount,
    lower: f.lowerBound,
    upper: f.upperBound,
  })) || [];

  const categoryData = categorySpending?.map((c: any) => ({
    name: c.categoryName,
    amount: c.totalAmount,
    count: c.transactionCount,
  })) || [];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <CardTitle>Spending Forecast</CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={forecastData}>
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="predicted"
                stroke="#8884d8"
                strokeWidth={2}
                name="Predicted"
              />
              <Line
                type="monotone"
                dataKey="lower"
                stroke="#82ca9d"
                strokeDasharray="5 5"
                name="Lower Bound"
              />
              <Line
                type="monotone"
                dataKey="upper"
                stroke="#ffc658"
                strokeDasharray="5 5"
                name="Upper Bound"
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <CardTitle>Category Distribution</CardTitle>
          <div className="flex flex-wrap gap-1">
            {ranges.map((r) => (
              <Button
                key={r.value}
                variant={range === r.value ? "default" : "outline"}
                size="sm"
                onClick={() => setRange(r.value)}
                className="h-7 text-xs"
              >
                {r.label}
              </Button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={340}>
            <PieChart>
              <Pie
                data={categoryData}
                dataKey="amount"
                nameKey="name"
                cx="50%"
                cy="42%"
                outerRadius={75}
                innerRadius={40}
                paddingAngle={2}
                labelLine={false}
                label={(entry: { name: string; percent: number }) =>
                  entry.percent > 0.05 ? `${(entry.percent * 100).toFixed(0)}%` : ""
                }
              >
                {categoryData.map((_: { name: string; amount: number; count: number }, index: number) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={PIE_COLORS[index % PIE_COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                formatter={(value: number, name: string) => [
                  new Intl.NumberFormat("en-CA", {
                    style: "currency",
                    currency: "CAD",
                  }).format(value),
                  name,
                ]}
              />
              <Legend
                layout="horizontal"
                verticalAlign="bottom"
                align="center"
                wrapperStyle={{ fontSize: "12px", paddingTop: "8px" }}
              />
            </PieChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
}
