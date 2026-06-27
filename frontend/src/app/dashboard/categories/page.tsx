"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/dashboard/page-header";
import {
  useCategories,
  useMerchantRules,
  useUncategorizedTransactions,
  deleteRule,
  deleteCategory,
  updateRule,
} from "@/hooks/use-categories";
import { useToast } from "@/components/toast-provider";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Trash2, AlertCircle, Tag, X, Pencil, Loader2 } from "lucide-react";

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
  const [editingRuleId, setEditingRuleId] = useState<number | null>(null);
  const [editingPattern, setEditingPattern] = useState("");
  const [editingError, setEditingError] = useState<string | null>(null);
  const [savingRuleId, setSavingRuleId] = useState<number | null>(null);

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

  function beginEditRule(ruleId: number, pattern: string) {
    setEditingRuleId(ruleId);
    setEditingPattern(pattern);
    setEditingError(null);
  }

  function cancelEditRule() {
    setEditingRuleId(null);
    setEditingPattern("");
    setEditingError(null);
  }

  async function handleSaveRule(ruleId: number) {
    if (editingPattern.trim().length === 0) {
      setEditingError("Rule pattern cannot be empty.");
      return;
    }

    setSavingRuleId(ruleId);
    setEditingError(null);
    try {
      await updateRule(ruleId, editingPattern);
      await mutateRules();
      showToast("Rule updated", "success");
      cancelEditRule();
    } catch (err: any) {
      const message = err?.message || "Failed to update rule";
      setEditingError(message);
      showToast(`Failed to update rule: ${message}`, "error");
    } finally {
      setSavingRuleId(null);
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
                  className="space-y-3 p-3 rounded-lg border bg-card"
                >
                  <div className="min-w-0 space-y-2">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Badge variant="outline">{rule.categoryName}</Badge>
                      <span className="text-xs text-muted-foreground">
                        Priority: {rule.priority}
                      </span>
                    </div>
                    {editingRuleId === rule.id ? (
                      <div className="space-y-2">
                        <label
                          htmlFor={`rule-pattern-${rule.id}`}
                          className="text-xs font-medium text-muted-foreground"
                        >
                          Rule pattern
                        </label>
                        <textarea
                          id={`rule-pattern-${rule.id}`}
                          value={editingPattern}
                          onChange={(event) => {
                            setEditingPattern(event.target.value);
                            if (editingError) setEditingError(null);
                          }}
                          rows={4}
                          className="min-h-24 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm leading-6 shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                          aria-invalid={Boolean(editingError)}
                        />
                        {editingError ? (
                          <p className="text-xs text-red-600">{editingError}</p>
                        ) : null}
                      </div>
                    ) : (
                      <code className="text-xs block whitespace-pre-wrap break-words text-muted-foreground">
                        {rule.pattern}
                      </code>
                    )}
                  </div>

                  {editingRuleId === rule.id ? (
                    <div className="flex flex-col sm:flex-row gap-2">
                      <Button
                        className="h-11 sm:h-10"
                        onClick={() => handleSaveRule(rule.id)}
                        disabled={savingRuleId === rule.id}
                      >
                        {savingRuleId === rule.id ? (
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        ) : null}
                        Save rule
                      </Button>
                      <Button
                        variant="outline"
                        className="h-11 sm:h-10"
                        onClick={cancelEditRule}
                        disabled={savingRuleId === rule.id}
                      >
                        Cancel
                      </Button>
                    </div>
                  ) : (
                    <div className="flex flex-col sm:flex-row gap-2">
                      <Button
                        variant="outline"
                        className="h-11 sm:h-10"
                        onClick={() => beginEditRule(rule.id, rule.pattern)}
                        disabled={deleting === rule.id}
                      >
                        <Pencil className="h-4 w-4 mr-2" />
                        Edit rule
                      </Button>
                      <Button
                        variant="ghost"
                        className="h-11 sm:h-10 text-red-600 hover:text-red-700 hover:bg-red-50"
                        disabled={deleting === rule.id}
                        onClick={() => handleDeleteRule(rule.id)}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete
                      </Button>
                    </div>
                  )}
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
