import { Transaction } from "@/types/transaction";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartConfig, ChartContainer } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, Tooltip } from "recharts";

interface TransactionChartProps {
  transactions: Transaction[];
}

export function TransactionChart({ transactions }: TransactionChartProps) {
  console.log("transactions", transactions);
  const chartData = transactions.reduce((acc, transaction) => {
    const date = transaction.transactionDate.split("T")[0];
    const amount = parseFloat(transaction.amount);
    acc[date] = (acc[date] || 0) + amount;
    return acc;
  }, {} as Record<string, number>);

  const data = Object.entries(chartData).map(([date, amount]) => ({
    date,
    amount,
  }));

  const chartConfig = {} satisfies ChartConfig;

  return (
    <Card className="col-span-2">
      <CardHeader>
        <CardTitle>Transaction History</CardTitle>
      </CardHeader>
      <CardContent className="h-[300px]">
        <ChartContainer config={chartConfig} className="w-full h-full">
          <BarChart data={data}>
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="amount" fill="#8884d8" />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
