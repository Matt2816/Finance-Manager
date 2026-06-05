"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/dashboard/page-header";
import { useCategories, useMerchantRules, useUncategorizedTransactions, deleteRule, deleteCategory } from "@/hooks/use-categories";
import { useToast } from "@/components/toast-provider";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Trash2, AlertCircle, Tag, X } from "lucide-react";

const SESSION_DISMISS_KEY = "uncategorized-prompt-session-dismissed";
const LOCAL_DISMISS_KEY = "uncategorized-prompt-dismissed";

export default function CategoriesPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const { categories, isLoading: catLoading, mutate: mutateCategories } = useCategories();
  const { rules, isLoading: rulesLoading, mutate: mutateRules } = useMerchantRules();
  const { uncategorized, isLoading: txLoading } = useUncategorizedTransactions();
  const [deleting, setDeleting] = useState<number | null>(null);
  const [deleteCatDialogOpen, setDeleteCatDialogOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState<{ id: number; name: string } | null>(null);

  const uncategorizedCount = uncategorized?.length ?? 0;

  function handleReview() {
    sessionStorage.removeItem(SESSION_DISMISS_KEY);
    localStorage.removeItem(LOCAL_DISMISS_KEY);
    router.push("/dashboard");
  }

  async function handleDeleteRule(ruleId: number) {
    setDeleting(ruleId);
    try {
      await deleteRule(ruleId);
      await mutateRules();
      showToast("Rule deleted", "success");
    } catch (err: any) {
      showToast("Failed to delete rule: " + err.message, "error");
    } finally {
      setDeleting(null);
    }
  }

  async function handleDeleteCategory() {
    if (!categoryToDelete) return;
    setDeleting(categoryToDelete.id);
    try {
      await deleteCategory(categoryToDelete.id);
      await Promise.all([
        mutateCategories(),
        mutateRules(),
      ]);
      showToast(`Category "${categoryToDelete.name}" deleted`, "success");
    } catch (err: any) {
      showToast("Failed to delete category: " + err.message, "error");
    } finally {
      setDeleting(null);
      setCategoryToDelete(null);
    }
  }

  if (catLoading || rulesLoading || txLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Categories" description="Manage spending categories and merchant rules." />
        <div className="h-96 bg-muted rounded-lg animate-pulse" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader
        title="Categories"
        description="Manage spending categories and merchant categorization rules."
      />

      {/* Uncategorized Review */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <Tag className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="font-medium">Uncategorized Transactions</p>
                <p className="text-sm text-muted-foreground">
                  {uncategorizedCount} transaction{uncategorizedCount !== 1 ? "s" : ""} need categorization
                </p>
              </div>
            </div>
            <Button
              size="sm"
              onClick={handleReview}
              disabled={uncategorizedCount === 0}
            >
              Review Now
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Categories */}
      <Card>
        <CardHeader>
          <CardTitle>Spending Categories</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            {categories
              ?.filter((c) => c.slug !== "uncategorized")
              .map((cat) => (
                <Badge
                  key={cat.id}
                  variant="secondary"
                  className="text-sm px-3 py-1.5 gap-1.5 flex items-center group"
                >
                  {cat.displayName}
                  <button
                    type="button"
                    disabled={deleting === cat.id}
                    onClick={() => {
                      setCategoryToDelete({ id: cat.id, name: cat.displayName });
                      setDeleteCatDialogOpen(true);
                    }}
                    className="ml-0.5 rounded-full p-0.5 opacity-50 hover:opacity-100 hover:bg-red-100 hover:text-red-600 transition-colors disabled:opacity-25"
                    title={`Delete ${cat.displayName}`}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
          </div>
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteCatDialogOpen}
        onOpenChange={(open) => {
          setDeleteCatDialogOpen(open);
          if (!open) setCategoryToDelete(null);
        }}
        title={`Delete "${categoryToDelete?.name ?? ""}"?`}
        description="Transactions in this category will be re-assigned to Uncategorized. Merchant rules for this category will also be removed."
        confirmLabel="Delete"
        onConfirm={handleDeleteCategory}
      />

      {/* Merchant Rules */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <CardTitle>Merchant Category Rules</CardTitle>
          <p className="text-sm text-muted-foreground">
            {rules?.length ?? 0} rule{(rules?.length ?? 0) !== 1 ? "s" : ""}
          </p>
        </CardHeader>
        <CardContent>
          {rules && rules.length > 0 ? (
            <div className="space-y-2">
              {rules.map((rule) => (
                <div
                  key={rule.id}
                  className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 p-3 rounded-lg border bg-card"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Badge variant="outline">{rule.categoryName}</Badge>
                      <span className="text-xs text-muted-foreground">
                        Priority: {rule.priority}
                      </span>
                    </div>
                    <code className="text-xs block break-all text-muted-foreground">
                      {rule.pattern}
                    </code>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-600 hover:text-red-700 hover:bg-red-50 shrink-0 self-end sm:self-auto"
                    disabled={deleting === rule.id}
                    onClick={() => handleDeleteRule(rule.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex items-center gap-2 text-sm text-muted-foreground py-8 justify-center">
              <AlertCircle className="h-4 w-4" />
              No merchant rules yet. Rules are created automatically when you categorize a transaction.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
