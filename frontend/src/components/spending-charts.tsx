"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCategorySpending, useForecasts } from "@/hooks/use-analytics";
import { Bar, BarChart, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export function SpendingCharts() {
  const { forecasts, isLoading: forecastsLoading } = useForecasts("TOTAL", 12);
  const { categorySpending, isLoading: categoryLoading } = useCategorySpending();

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
        <CardHeader>
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
        <CardHeader>
          <CardTitle>Category Distribution</CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={categoryData}>
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="amount" fill="#8884d8" name="Total Amount" />
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
}
