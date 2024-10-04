import { Transaction } from "@/types/transaction";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface TransactionSummaryProps {
  transactions: Transaction[];
}

export function TransactionSummary({ transactions }: TransactionSummaryProps) {
  console.log("transactions", transactions);
  const parsedAmounts = transactions.map((transaction) =>
    parseFloat(transaction.amount.replace("$", ""))
  );
  console.log("Parsed amounts:", parsedAmounts);

  const totalAmount = parsedAmounts.reduce((sum, amount) => sum + amount, 0);
  const averageAmount = totalAmount / transactions.length;

  return (
    <div className="grid grid-cols-2 gap-4">
      <Card>
        <CardHeader>
          <CardTitle>Total Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-bold">{transactions.length}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Total Amount</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-bold">${totalAmount.toFixed(2)}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Average Transaction</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-bold">${averageAmount.toFixed(2)}</p>
        </CardContent>
      </Card>
    </div>
  );
}
