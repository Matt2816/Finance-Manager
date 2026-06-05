"use client";

import { useState, useMemo } from "react";
import { PageHeader } from "@/components/dashboard/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useIncomeSummary } from "@/hooks/use-analytics";
import {
  useRecurringTransactions,
  pauseRecurring,
  resumeRecurring,
  generateNowRecurring,
} from "@/hooks/use-recurring";
import { useToast } from "@/components/toast-provider";
import {
  ArrowDownLeft,
  ArrowUpRight,
  Wallet,
  TrendingUp,
  CalendarClock,
  Play,
  Pause,
  Plus,
  Zap,
} from "lucide-react";
import { RecurringFormDialog } from "@/components/recurring-form-dialog";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-CA", {
    style: "currency",
    currency: "CAD",
    maximumFractionDigits: 0,
  }).format(value);
}

export default function IncomePage() {
  const { showToast } = useToast();
  const { summary: backendSummary, isLoading: summaryLoading, mutate: refreshSummary } = useIncomeSummary();
  const [direction, setDirection] = useState<"all" | "DEBIT" | "CREDIT">("all");
  const [statusFilter, setStatusFilter] = useState<"all" | "active" | "paused">("active");
  const { recurring, isLoading: recurringLoading, mutate } = useRecurringTransactions();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<any>(null);

  async function handleToggleActive(rt: any) {
    try {
      if (rt.active) {
        await pauseRecurring(rt.id);
        showToast("Paused recurring transaction", "success");
      } else {
        await resumeRecurring(rt.id);
        showToast("Resumed recurring transaction", "success");
      }
      mutate();
      refreshSummary();
    } catch (err: any) {
      showToast(err.message, "error");
    }
  }

  async function handleGenerateNow(rt: any) {
    try {
      await generateNowRecurring(rt.id);
      showToast(`Generated transactions for "${rt.name}"`, "success");
      mutate();
      refreshSummary();
    } catch (err: any) {
      showToast(err.message, "error");
    }
  }

  const displayList = useMemo(() => {
    if (!recurring) return [];
    return recurring.filter((rt: any) => {
      const dirMatch = direction === "all" || rt.direction === direction;
      const statusMatch =
        statusFilter === "all" ||
        (statusFilter === "active" && rt.active) ||
        (statusFilter === "paused" && !rt.active);
      return dirMatch && statusMatch;
    });
  }, [recurring, direction, statusFilter]);

  const summary = useMemo(() => {
    const getMonthly = (r: any) => {
      if (r.monthlyEquivalent) return parseFloat(String(r.monthlyEquivalent));
      const multipliers: Record<string, number> = { WEEKLY: 52 / 12, BIWEEKLY: 26 / 12, MONTHLY: 1 };
      return parseFloat(String(r.amount)) * (multipliers[r.frequency] ?? 1);
    };

    const totalIncome = (recurring ?? [])
      .filter((r: any) => r.direction === "CREDIT" && r.active)
      .reduce((sum: number, r: any) => sum + getMonthly(r), 0);

    const totalExpenses = (recurring ?? [])
      .filter((r: any) => r.direction === "DEBIT" && r.active)
      .reduce((sum: number, r: any) => sum + getMonthly(r), 0);

    const netSavings = totalIncome - totalExpenses;
    const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;
    return { totalIncome, totalExpenses, netSavings, savingsRate };
  }, [recurring]);

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader
        title="Income & Recurring"
        description="Manage recurring income and expenses, and view your financial summary."
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
              <ArrowDownLeft className="h-5 w-5 text-green-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Total Income</p>
              <p className="text-xl font-bold">
                {recurringLoading || summaryLoading ? "—" : formatCurrency(summary.totalIncome)}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-red-100 flex items-center justify-center">
              <ArrowUpRight className="h-5 w-5 text-red-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Total Expenses</p>
              <p className="text-xl font-bold">
                {recurringLoading || summaryLoading ? "—" : formatCurrency(summary.totalExpenses)}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
              <Wallet className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Net Savings</p>
              <p className="text-xl font-bold">
                {recurringLoading || summaryLoading ? "—" : formatCurrency(summary.netSavings)}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-amber-100 flex items-center justify-center">
              <TrendingUp className="h-5 w-5 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Savings Rate</p>
              <p className="text-xl font-bold">
                {recurringLoading || summaryLoading ? "—" : `${summary.savingsRate.toFixed(1)}%`}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recurring Transactions */}
      <Card>
        <CardHeader className="flex flex-col gap-3">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <CardTitle>Recurring Transactions</CardTitle>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <div className="flex gap-1">
                {(["all", "CREDIT", "DEBIT"] as const).map((d) => (
                  <Button
                    key={d}
                    variant={direction === d ? "default" : "outline"}
                    size="sm"
                    className="h-8 text-xs px-3"
                    onClick={() => setDirection(d)}
                  >
                    {d === "all" ? "All" : d === "CREDIT" ? "Income" : "Expense"}
                  </Button>
                ))}
              </div>
              <Button
                size="sm"
                className="h-8 gap-1"
                onClick={() => {
                  setEditing(null);
                  setFormOpen(true);
                }}
              >
                <Plus className="h-4 w-4" />
                <span className="hidden sm:inline">Add</span>
              </Button>
            </div>
          </div>
          <div className="flex gap-1">
            {(["active", "paused", "all"] as const).map((s) => (
              <Button
                key={s}
                variant={statusFilter === s ? "secondary" : "ghost"}
                size="sm"
                className="h-7 text-xs px-2"
                onClick={() => setStatusFilter(s)}
              >
                {s === "active" ? "Active" : s === "paused" ? "Paused" : "All"}
              </Button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          {recurringLoading ? (
            <div className="h-32 bg-muted rounded-lg animate-pulse" />
          ) : displayList.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground text-sm">
              No recurring transactions match your filters.
            </div>
          ) : (
            <div className="space-y-2">
              {displayList.map((rt) => (
                <div
                  key={rt.id}
                  className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 p-3 rounded-lg border bg-card"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium text-sm">{rt.name}</span>
                      <Badge
                        variant={rt.direction === "CREDIT" ? "default" : "destructive"}
                        className="text-[10px] h-5"
                      >
                        {rt.direction === "CREDIT" ? "Income" : "Expense"}
                      </Badge>
                      {!rt.active && (
                        <Badge variant="outline" className="text-[10px] h-5">
                          Paused
                        </Badge>
                      )}
                    </div>
                    <div className="flex items-center gap-3 text-xs text-muted-foreground">
                      <span>
                        {new Intl.NumberFormat("en-CA", {
                          style: "currency",
                          currency: "CAD",
                        }).format(parseFloat(rt.amount))}
                      </span>
                      <span className="flex items-center gap-1">
                        <CalendarClock className="h-3 w-3" />
                        {rt.frequency.toLowerCase().replace("ly", "ly")}
                      </span>
                      <span>Starts {rt.startDate}</span>
                      {rt.endDate && <span>Ends {rt.endDate}</span>}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 self-end sm:self-auto">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      title="Generate now"
                      onClick={() => handleGenerateNow(rt)}
                    >
                      <Zap className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => handleToggleActive(rt)}
                    >
                      {rt.active ? (
                        <Pause className="h-4 w-4" />
                      ) : (
                        <Play className="h-4 w-4" />
                      )}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 text-xs"
                      onClick={() => {
                        setEditing(rt);
                        setFormOpen(true);
                      }}
                    >
                      Edit
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <RecurringFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        initialData={editing}
        onSuccess={() => {
          mutate();
          refreshSummary();
          setEditing(null);
        }}
      />
    </div>
  );
}
