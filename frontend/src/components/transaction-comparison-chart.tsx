"use client";

import { useMemo } from "react";
import { TrendingUp, TrendingDown } from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  XAxis,
  YAxis,
  ResponsiveContainer,
} from "recharts";

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";
import { Transaction } from "@/types/transaction";

export const description =
  "An area chart showing cumulative spending and projected amount";

const chartConfig = {
  thisMonth: {
    label: "This Month",
    color: "hsl(var(--chart-1))",
  },
  lastMonth: {
    label: "Last Month",
    color: "hsl(var(--chart-2))",
  },
  projected: {
    label: "Projected",
    color: "hsl(var(--chart-3))",
  },
} satisfies ChartConfig;

export function TransactionComparisonChart({
  transactions,
}: {
  transactions: Transaction[];
}) {
  const chartData = useMemo(() => {
    const now = new Date();
    const thisMonth = now.getMonth();
    const lastMonth = thisMonth === 0 ? 11 : thisMonth - 1;
    const thisYear = now.getFullYear();
    const lastYear = thisMonth === 0 ? thisYear - 1 : thisYear;

    const getDaysInMonth = (year: number, month: number) =>
      new Date(year, month + 1, 0).getDate();

    const thisMonthDays = getDaysInMonth(thisYear, thisMonth);
    const lastMonthDays = getDaysInMonth(lastYear, lastMonth);

    const allDays = Array.from(
      { length: Math.max(thisMonthDays, lastMonthDays) },
      (_, i) => i + 1
    );

    const initialData = allDays.reduce((acc, day) => {
      acc[day] = { day, thisMonth: 0, lastMonth: 0, projected: null };
      return acc;
    }, {} as Record<number, { day: number; thisMonth: number; lastMonth: number; projected: number | null }>);

    transactions.forEach((transaction) => {
      const date = new Date(transaction.transactionDate);
      const month = date.getMonth();
      const year = date.getFullYear();
      const day = date.getDate();

      const amount = parseFloat(transaction.amount.replace(/[^0-9.-]+/g, ""));

      if (month === thisMonth && year === thisYear) {
        for (let i = day; i <= thisMonthDays; i++) {
          initialData[i].thisMonth += amount;
        }
      } else if (month === lastMonth && year === lastYear) {
        for (let i = day; i <= lastMonthDays; i++) {
          initialData[i].lastMonth += amount;
        }
      }
    });

    // Extend last month's data by one day if this month is longer
    if (thisMonthDays > lastMonthDays) {
      initialData[lastMonthDays + 1].lastMonth =
        initialData[lastMonthDays].lastMonth;
    }

    // Calculate projected spending
    const currentDay = now.getDate();
    const projectionRate = initialData[currentDay].thisMonth / currentDay;
    let projectedAmount = initialData[currentDay].thisMonth;

    for (let day = currentDay + 1; day <= thisMonthDays; day++) {
      projectedAmount += projectionRate;
      initialData[day].projected = projectedAmount;
    }

    // Convert amounts to dollar amounts and round to the nearest tenth
    return Object.values(initialData).map((data) => ({
      ...data,
      thisMonth: Math.round(data.thisMonth * 100) / 100,
      lastMonth: Math.round(data.lastMonth * 100) / 100,
      projected:
        data.projected !== null ? Math.round(data.projected * 100) / 100 : null,
    }));
  }, [transactions]);

  console.log("chartData", chartData);

  const totalThisMonth =
    chartData[chartData.length - 1].projected ||
    chartData[chartData.length - 1].thisMonth;
  const totalLastMonth = chartData[chartData.length - 1].lastMonth;
  const percentageChange =
    ((totalThisMonth - totalLastMonth) / totalLastMonth) * 100;

  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle>Cumulative Spending Comparison</CardTitle>
        <CardDescription>
          Comparing cumulative spending for this month and last month, with
          projection
        </CardDescription>
      </CardHeader>
      <CardContent className="p-0">
        <ChartContainer config={chartConfig} className="h-[300px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart
              accessibilityLayer
              data={chartData}
              margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="day"
                type="number"
                domain={[1, "dataMax"]}
                tickFormatter={(value) => value.toString()}
              />
              <YAxis />
              <ChartTooltip content={<ChartTooltipContent />} />
              <defs>
                <linearGradient id="colorThisMonth" x1="0" y1="0" x2="0" y2="1">
                  <stop
                    offset="5%"
                    stopColor={chartConfig.thisMonth.color}
                    stopOpacity={0.8}
                  />
                  <stop
                    offset="95%"
                    stopColor={chartConfig.thisMonth.color}
                    stopOpacity={0.1}
                  />
                </linearGradient>
                <linearGradient id="colorLastMonth" x1="0" y1="0" x2="0" y2="1">
                  <stop
                    offset="5%"
                    stopColor={chartConfig.lastMonth.color}
                    stopOpacity={0.8}
                  />
                  <stop
                    offset="95%"
                    stopColor={chartConfig.lastMonth.color}
                    stopOpacity={0.1}
                  />
                </linearGradient>
              </defs>
              <Area
                type="monotone"
                dataKey="thisMonth"
                stroke={chartConfig.thisMonth.color}
                fillOpacity={1}
                fill="url(#colorThisMonth)"
              />
              <Area
                type="monotone"
                dataKey="projected"
                stroke={chartConfig.projected.color}
                fillOpacity={1}
                fill="url(#colorThisMonth)"
                strokeDasharray="5 5"
              />
              <Area
                type="monotone"
                dataKey="lastMonth"
                stroke={chartConfig.lastMonth.color}
                fillOpacity={1}
                fill="url(#colorLastMonth)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </ChartContainer>
      </CardContent>
      <CardFooter>
        <div className="flex w-full items-start gap-2 text-sm">
          <div className="grid gap-2">
            <div className="flex items-center gap-2 font-medium leading-none">
              {percentageChange >= 0 ? (
                <>
                  Projected to increase by {percentageChange.toFixed(1)}% this
                  month <TrendingUp className="h-4 w-4 text-green-500" />
                </>
              ) : (
                <>
                  Projected to decrease by{" "}
                  {Math.abs(percentageChange).toFixed(1)}% this month{" "}
                  <TrendingDown className="h-4 w-4 text-red-500" />
                </>
              )}
            </div>
            <div className="flex items-center gap-2 leading-none text-muted-foreground">
              This Month (Projected): ${totalThisMonth.toFixed(1)} | Last Month:
              ${totalLastMonth.toFixed(1)}
            </div>
          </div>
        </div>
      </CardFooter>
    </Card>
  );
}
