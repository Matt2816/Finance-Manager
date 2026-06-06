"use client";

import { useState, useMemo } from "react";
import { PageHeader } from "@/components/dashboard/page-header";
import { TransactionTable } from "@/components/transaction-table";
import { TransactionSummary } from "@/components/transaction-summary";
import { TransactionChart } from "@/components/transaction-chart";
import { TransactionComparisonChart } from "@/components/transaction-comparison-chart";
import { columns } from "@/components/columns";
import { useTransactions } from "@/hooks/use-transactions";
import { useCategories, assignCategory, type Category } from "@/hooks/use-categories";
import { useToast } from "@/components/toast-provider";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch } from "@/lib/authenticated-fetch";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/confirm-dialog";
import {
  TransactionFormDialog,
  AddTransactionButton,
} from "@/components/transaction-form-dialog";
import type { Transaction } from "@/types/transaction";

type Range = "MTD" | "1M" | "3M" | "6M" | "1Y" | "All";

const ranges: { value: Range; label: string }[] = [
  { value: "MTD", label: "Month to Date" },
  { value: "1M", label: "1 Month" },
  { value: "3M", label: "3 Months" },
  { value: "6M", label: "6 Months" },
  { value: "1Y", label: "1 Year" },
  { value: "All", label: "All Time" },
];

function getStartDate(range: Range): Date | null {
  if (range === "All") return null;
  const now = new Date();
  const d = new Date(now);
  d.setHours(0, 0, 0, 0);
  switch (range) {
    case "MTD":
      d.setDate(1);
      break;
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
  }
  return d;
}

function parseLocalDate(dateStr: string): Date {
  return new Date(dateStr + "T00:00:00");
}

function filterTransactions(transactions: Transaction[], range: Range) {
  const startDate = getStartDate(range);
  if (!startDate) return transactions;
  return transactions.filter((t) => parseLocalDate(t.transactionDate) >= startDate);
}

export default function TransactionsPage() {
  const { transactions, isLoading, error, mutate } = useTransactions();
  const { categories } = useCategories();
  const { showToast } = useToast();
  const [range, setRange] = useState<Range>("MTD");
  const [showUncategorizedOnly, setShowUncategorizedOnly] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"add" | "edit">("add");
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const filtered = useMemo(() => {
    let data = filterTransactions(transactions ?? [], range);
    if (showUncategorizedOnly) {
      data = data.filter((t) => {
        if (t.categoryId == null) return true;
        const cat = categories?.find((c: Category) => c.id === t.categoryId);
        return cat?.displayName?.toLowerCase() === "uncategorized";
      });
    }
    return data;
  }, [transactions, range, showUncategorizedOnly, categories]);

  async function handleCategorize(tx: Transaction, categoryId: number) {
    try {
      await assignCategory(tx.id, categoryId, true);
      mutate();
      showToast("Transaction categorized", "success");
    } catch (err: any) {
      showToast("Failed to categorize: " + err.message, "error");
    }
  }

  function handleAdd() {
    setDialogMode("add");
    setEditingTransaction(null);
    setDialogOpen(true);
  }

  function handleEdit(tx: Transaction) {
    setDialogMode("edit");
    setEditingTransaction(tx);
    setDialogOpen(true);
  }

  function handleDeletePrompt(tx: Transaction) {
    setDeleteTarget(tx);
    setDeleteOpen(true);
  }

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    try {
      const res = await authenticatedFetch(
        `${getApiBaseUrl()}/api/transaction/${deleteTarget.id}`,
        { method: "DELETE" }
      );
      if (!res.ok) throw new Error(await res.text());
      mutate();
      showToast("Transaction deleted", "success");
    } catch (err: any) {
      showToast("Failed to delete transaction: " + err.message, "error");
    } finally {
      setDeleteOpen(false);
      setDeleteTarget(null);
    }
  }

  function handleSuccess() {
    mutate();
    showToast(dialogMode === "edit" ? "Transaction updated" : "Transaction created", "success");
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Transactions" description="View and filter all your transactions." />
        <div className="h-96 bg-muted rounded-lg animate-pulse" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader title="Transactions" description="View and filter all your transactions." />
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            Error loading transactions: {error.message}
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!transactions?.length) {
    return (
      <div className="space-y-6">
        <PageHeader title="Transactions" description="View and filter all your transactions." />
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No transactions yet. Add data via the backend API.
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <PageHeader
          title="Transactions"
          description="View, search, and filter all your financial transactions."
        />
        <AddTransactionButton onClick={handleAdd} />
      </div>

      <TransactionSummary transactions={filtered} />

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
            <Button
              variant={showUncategorizedOnly ? "default" : "outline"}
              size="sm"
              onClick={() => setShowUncategorizedOnly((v: boolean) => !v)}
              className="h-7 text-xs"
            >
              Uncategorized
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <TransactionTable
            columns={columns}
            data={filtered}
            onEdit={handleEdit}
            onDelete={handleDeletePrompt}
            onCategorize={handleCategorize}
            categories={categories}
          />
        </CardContent>
      </Card>

      <div className="space-y-6">
        <TransactionChart transactions={filtered} />
        <TransactionComparisonChart transactions={filtered} />
      </div>

      <TransactionFormDialog
        mode={dialogMode}
        transaction={editingTransaction}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSuccess={handleSuccess}
        categories={categories}
      />

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Delete Transaction"
        description={`Are you sure you want to delete "${deleteTarget?.name ?? ""}"? This action cannot be undone.`}
        confirmLabel="Delete"
        onConfirm={handleConfirmDelete}
      />
    </div>
  );
}
