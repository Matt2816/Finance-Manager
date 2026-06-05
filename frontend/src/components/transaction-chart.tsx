"use client";

import { useState, useMemo } from "react";
import { Transaction } from "@/types/transaction";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartConfig, ChartContainer } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, Tooltip } from "recharts";
import { Button } from "@/components/ui/button";

type Range = "1M" | "3M" | "6M" | "1Y" | "All";

interface TransactionChartProps {
  transactions: Transaction[];
}

const ranges: { value: Range; label: string }[] = [
  { value: "1M", label: "1 Month" },
  { value: "3M", label: "3 Months" },
  { value: "6M", label: "6 Months" },
  { value: "1Y", label: "1 Year" },
  { value: "All", label: "All Time" },
];

function getStartDate(range: Range): Date {
  const now = new Date();
  const d = new Date(now);
  switch (range) {
    case "1M":
      d.setMonth(d.getMonth() - 1);
      break;
    case "3M":
      d.setMonth(d.getMonth() - 3);
      break;
    case "6M":
      d.setMonth(d.getMonth() - 6);
      break;
    case "1Y":
      d.setFullYear(d.getFullYear() - 1);
      break;
    case "All":
      return new Date(0);
  }
  return d;
}

export function TransactionChart({ transactions }: TransactionChartProps) {
  const [range, setRange] = useState<Range>("1Y");
  const startDate = getStartDate(range);
  const isMonthly = range === "1Y" || range === "All";

  const filtered = useMemo(
    () =>
      transactions.filter((t) => {
        const d = new Date(t.transactionDate);
        return d >= startDate;
      }),
    [transactions, startDate]
  );

  const chartData = useMemo(() => {
    const dataMap = filtered.reduce((acc: Record<string, number>, transaction) => {
      const dateStr = transaction.transactionDate.split("T")[0];
      const key = isMonthly ? dateStr.slice(0, 7) : dateStr;
      const amount = parseFloat(transaction.amount);
      acc[key] = (acc[key] || 0) + amount;
      return acc;
    }, {} as Record<string, number>);

    const entries = Object.entries(dataMap).map(([key, amount]) => ({
      key,
      amount,
    }));

    entries.sort((a, b) => a.key.localeCompare(b.key));
    return entries;
  }, [filtered, isMonthly]);

  const formatTick = (key: string) => {
    if (isMonthly) {
      const [year, month] = key.split("-");
      return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString("en-US", {
        month: "short",
        year: "2-digit",
      });
    }
    const d = new Date(key);
    return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  };

  const chartConfig = {} satisfies ChartConfig;

  return (
    <Card>
      <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <CardTitle>Transaction History</CardTitle>
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
      <CardContent className="h-[300px]">
        <ChartContainer config={chartConfig} className="w-full h-full">
          <BarChart data={chartData}>
            <XAxis
              dataKey="key"
              tickFormatter={formatTick}
              interval="preserveStartEnd"
              minTickGap={20}
            />
            <YAxis />
            <Tooltip
              formatter={(value: number) => [`$${value.toFixed(2)}`, "Amount"]}
              labelFormatter={(label: string) => {
                if (isMonthly) {
                  const [year, month] = label.split("-");
                  return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString("en-US", {
                    month: "long",
                    year: "numeric",
                  });
                }
                return new Date(label).toLocaleDateString();
              }}
            />
            <Bar dataKey="amount" fill="hsl(var(--chart-1))" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
