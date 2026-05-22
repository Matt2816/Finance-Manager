import { Transaction } from "@/types/transaction";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { parseAmount } from "@/lib/transaction-analytics";

interface TransactionSummaryProps {
  transactions: Transaction[];
}

export function TransactionSummary({ transactions }: TransactionSummaryProps) {
  const parsedAmounts = transactions.map((transaction) =>
    parseAmount(transaction.amount)
  );

  const totalAmount = parsedAmounts.reduce((sum, amount) => sum + amount, 0);
  const averageAmount =
    transactions.length > 0 ? totalAmount / transactions.length : 0;

  return (
    <div className="grid grid-cols-1 gap-3 min-[400px]:grid-cols-2 sm:gap-4">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            Total Transactions
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-2xl font-bold sm:text-3xl">{transactions.length}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            Total Amount
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-2xl font-bold sm:text-3xl">
            ${totalAmount.toFixed(2)}
          </p>
        </CardContent>
      </Card>
      <Card className="min-[400px]:col-span-2 sm:col-span-1">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            Average Transaction
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-2xl font-bold sm:text-3xl">
            ${averageAmount.toFixed(2)}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
