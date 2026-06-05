"use client";

import { useState, useEffect } from "react";
import { Tag, ChevronLeft, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  useCategories,
  useUncategorizedTransactions,
  assignCategory,
  createCategory,
  type UncategorizedTransaction,
} from "@/hooks/use-categories";
import { useToast } from "@/components/toast-provider";
import { cn } from "@/lib/utils";

const SESSION_DISMISS_KEY = "uncategorized-prompt-session-dismissed";
const LOCAL_DISMISS_KEY = "uncategorized-prompt-dismissed";

export function UncategorizedPrompt() {
  const { showToast } = useToast();
  const [open, setOpen] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [dontShowAgain, setDontShowAgain] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [assigning, setAssigning] = useState(false);
  const [queue, setQueue] = useState<UncategorizedTransaction[]>([]);
  const [showAddCategory, setShowAddCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [creatingCategory, setCreatingCategory] = useState(false);

  const { categories, mutate: mutateCategories } = useCategories();
  const { uncategorized, mutate: mutateUncategorized } =
    useUncategorizedTransactions();

  useEffect(() => {
    const sessionDismissed = sessionStorage.getItem(SESSION_DISMISS_KEY);
    const localDismissed = localStorage.getItem(LOCAL_DISMISS_KEY);
    if (sessionDismissed || localDismissed) {
      setDismissed(true);
    }
  }, []);

  useEffect(() => {
    if (!dismissed && uncategorized && uncategorized.length > 0 && !open) {
      setQueue(uncategorized);
      setCurrentIndex(0);
      setOpen(true);
    }
  }, [dismissed, uncategorized, open]);

  function handleDismiss() {
    setOpen(false);
    sessionStorage.setItem(SESSION_DISMISS_KEY, "true");
    if (dontShowAgain) {
      localStorage.setItem(LOCAL_DISMISS_KEY, "true");
    }
    setDismissed(true);
  }

  async function handleAssign(categoryId: number) {
    if (queue.length === 0 || currentIndex >= queue.length) return;
    const tx = queue[currentIndex];
    setAssigning(true);
    try {
      await assignCategory(tx.id, categoryId, true);
      await mutateUncategorized();
      showToast("Transaction categorized", "success");

      setQueue((prev) => {
        const next = prev.filter((_, i) => i !== currentIndex);
        if (next.length === 0) {
          handleDismiss();
          return next;
        }
        // If we removed the last item, adjust index to the new last
        if (currentIndex >= next.length) {
          setCurrentIndex(next.length - 1);
        }
        return next;
      });
    } finally {
      setAssigning(false);
    }
  }

  async function handleSkip() {
    if (queue.length <= 1 || currentIndex >= queue.length - 1) {
      handleDismiss();
      return;
    }
    setCurrentIndex((i) => i + 1);
  }

  async function handleCreateCategory() {
    const name = newCategoryName.trim();
    if (!name) return;
    setCreatingCategory(true);
    try {
      const slug = name
        .toLowerCase()
        .replace(/\s+/g, "-")
        .replace(/[^a-z0-9-]/g, "");
      await createCategory(slug || "custom", name);
      await mutateCategories();
      showToast(`Category "${name}" created`, "success");
      setNewCategoryName("");
      setShowAddCategory(false);
    } catch (err: any) {
      showToast("Failed to create category: " + err.message, "error");
    } finally {
      setCreatingCategory(false);
    }
  }

  const currentTx = queue[currentIndex];
  const total = queue.length;
  const progress = total > 0 ? currentIndex + 1 : 0;

  if (!open || !currentTx) return null;

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) handleDismiss();
      }}
    >
      <DialogContent className="max-h-[90dvh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <Tag className="h-5 w-5 text-primary shrink-0" />
            <DialogTitle className="text-base sm:text-lg">Categorize Transactions</DialogTitle>
          </div>
          <DialogDescription className="text-xs sm:text-sm text-left">
            {total} transaction{total !== 1 ? "s" : ""} need categorization.
          </DialogDescription>
        </DialogHeader>

        {/* Progress */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>
            {progress} of {total}
          </span>
          <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-primary transition-all duration-300"
              style={{ width: `${(progress / total) * 100}%` }}
            />
          </div>
        </div>

        {/* Transaction Card */}
        <div className="rounded-lg border bg-card p-3 sm:p-4 space-y-2 sm:space-y-3">
          <div className="flex items-start justify-between gap-2 min-w-0">
            <div className="min-w-0">
              <p className="font-medium break-words sm:truncate">{currentTx.name}</p>       
            </div>
            <p className="shrink-0 font-semibold text-sm">
              {new Intl.NumberFormat("en-CA", {
                style: "currency",
                currency: "CAD",
              }).format(parseFloat(currentTx.amount))}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
            <span>
              {new Date(currentTx.transactionDate).toLocaleDateString()}
            </span>
          </div>
        </div>

        {/* Category Buttons */}
        <div className="space-y-2">
          <p className="text-xs sm:text-sm font-medium">Select a category</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2 max-h-48 overflow-y-auto p-1 min-w-0">
            {categories
              ?.filter((c) => c.slug !== "uncategorized")
              .map((category) => (
                <Button
                  key={category.id}
                  variant="outline"
                  size="sm"
                  disabled={assigning}
                  onClick={() => handleAssign(category.id)}
                  className={cn(
                    "h-auto py-2 justify-start text-left w-full",
                    assigning && "opacity-50"
                  )}
                >
                  {category.displayName}
                </Button>
              ))}
          </div>

          {/* Add New Category */}
          {!showAddCategory ? (
            <Button
              variant="ghost"
              size="sm"
              className="w-full text-muted-foreground h-8 sm:h-9"
              onClick={() => setShowAddCategory(true)}
            >
              <Plus className="h-4 w-4 mr-1" />
              New category
            </Button>
          ) : (
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
              <Input
                placeholder="Category name"
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleCreateCategory();
                }}
                disabled={creatingCategory}
                className="h-8 sm:h-9"
              />
              <div className="flex items-center gap-2 shrink-0">
                <Button
                  size="sm"
                  className="h-8 sm:h-9 flex-1 sm:flex-none"
                  onClick={handleCreateCategory}
                  disabled={creatingCategory || !newCategoryName.trim()}
                >
                  Add
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 sm:h-9 flex-1 sm:flex-none"
                  onClick={() => {
                    setShowAddCategory(false);
                    setNewCategoryName("");
                  }}
                  disabled={creatingCategory}
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-2 border-t">
          <div className="flex items-center gap-2">
            <Checkbox
              id="dont-show"
              checked={dontShowAgain}
              onCheckedChange={(v) => setDontShowAgain(v === true)}
            />
            <Label htmlFor="dont-show" className="text-xs sm:text-sm cursor-pointer">
              Don&apos;t show again
            </Label>
          </div>
          <div className="flex items-center gap-2 justify-end">
            {currentIndex > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setCurrentIndex((i) => i - 1)}
                disabled={assigning}
                className="h-8 sm:h-9"
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Back
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              onClick={handleSkip}
              disabled={assigning}
              className="h-8 sm:h-9 flex-1 sm:flex-none"
            >
              Skip
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleDismiss}
              disabled={assigning}
              className="h-8 sm:h-9 flex-1 sm:flex-none"
            >
              Dismiss
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
