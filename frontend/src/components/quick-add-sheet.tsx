"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { Cross2Icon } from "@radix-ui/react-icons";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Dialog, DialogPortal, DialogOverlay } from "@/components/ui/dialog";
import { useToast } from "@/components/toast-provider";
import { useCategories, type Category } from "@/hooks/use-categories";
import { enqueue } from "@/lib/offline-queue";
import { buildQuickAddPayload } from "@/lib/wallet-note-payload";
import {
  syncPendingTransactions,
  PENDING_CHANGED_EVENT,
} from "@/lib/sync-pending-transactions";
import { ensureNotificationPermission } from "@/lib/sync-notifications";

const RECENT_KEY = "quick-add-recent-categories";
const RECENT_MAX = 5;

interface QuickAddSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function getRecentIds(): number[] {
  if (typeof localStorage === "undefined") return [];
  try {
    const raw = localStorage.getItem(RECENT_KEY);
    return raw ? (JSON.parse(raw) as number[]) : [];
  } catch {
    return [];
  }
}

function pushRecentId(id: number) {
  if (typeof localStorage === "undefined") return;
  const next = [id, ...getRecentIds().filter((x) => x !== id)].slice(0, RECENT_MAX);
  localStorage.setItem(RECENT_KEY, JSON.stringify(next));
}

export function QuickAddSheet({ open, onOpenChange }: QuickAddSheetProps) {
  const { categories } = useCategories();
  const { showToast } = useToast();
  const [amount, setAmount] = useState("");
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [showAll, setShowAll] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const amountRef = useRef<HTMLInputElement>(null);

  const selectable = useMemo(
    () => (categories ?? []).filter((c) => c.slug !== "uncategorized"),
    [categories]
  );

  const recentCategories = useMemo(() => {
    const recentIds = getRecentIds();
    const byId = new Map(selectable.map((c) => [c.id, c]));
    return recentIds.map((id) => byId.get(id)).filter((c): c is Category => Boolean(c));
  }, [selectable, open]);

  const chips = showAll || recentCategories.length === 0 ? selectable : recentCategories;

  useEffect(() => {
    if (open) {
      setAmount("");
      setCategoryId(null);
      setShowAll(false);
      const t = setTimeout(() => amountRef.current?.focus(), 120);
      return () => clearTimeout(t);
    }
  }, [open]);

  const canSave = amount.trim() !== "" && Number(amount.replace(/[^0-9.\-]/g, "")) > 0 && categoryId !== null;

  async function handleSave() {
    if (!canSave || categoryId === null) return;
    setSubmitting(true);
    const category = selectable.find((c) => c.id === categoryId);
    const payload = buildQuickAddPayload({
      amount,
      categoryId,
      categoryLabel: category?.displayName ?? "Cash expense",
    });

    try {
      await enqueue(payload);
      pushRecentId(categoryId);
      void ensureNotificationPermission();
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent(PENDING_CHANGED_EVENT));
      }
      onOpenChange(false);

      const online = typeof navigator === "undefined" ? true : navigator.onLine;
      if (online) {
        const outcome = await syncPendingTransactions((m, t) => showToast(m, t));
        if (outcome.created === 0 && outcome.duplicates === 0 && outcome.remaining > 0) {
          showToast("Saved — will sync automatically", "success");
        }
      } else {
        showToast("Saved offline — will sync automatically", "success");
      }
    } catch {
      showToast("Could not save expense", "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogOverlay />
        <DialogPrimitive.Content
          className={cn(
            "fixed inset-x-0 bottom-0 z-50 rounded-t-2xl border bg-background p-4 shadow-lg",
            "pb-[calc(1rem+env(safe-area-inset-bottom))]",
            "duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out",
            "data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
            "data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom"
          )}
          onOpenAutoFocus={(e) => e.preventDefault()}
        >
          <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-muted" aria-hidden="true" />
          <DialogPrimitive.Title className="text-lg font-semibold mb-1 px-1">
            Quick add expense
          </DialogPrimitive.Title>
          <DialogPrimitive.Description className="text-sm text-muted-foreground mb-4 px-1">
            Enter an amount and pick a category.
          </DialogPrimitive.Description>

          <div className="space-y-4">
            <div>
              <label htmlFor="quick-amount" className="block text-sm font-medium mb-1.5 px-1">
                Amount
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-lg text-muted-foreground">
                  $
                </span>
                <input
                  id="quick-amount"
                  ref={amountRef}
                  inputMode="decimal"
                  type="text"
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full rounded-lg border bg-background pl-8 pr-3 py-3 text-2xl font-semibold tabular-nums focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5 px-1">
                <span className="text-sm font-medium">Category</span>
                {recentCategories.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowAll((v) => !v)}
                    className="text-xs text-primary font-medium"
                  >
                    {showAll ? "Show recent" : "Show all"}
                  </button>
                )}
              </div>
              <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto">
                {chips.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setCategoryId(c.id)}
                    aria-pressed={categoryId === c.id}
                    className={cn(
                      "rounded-full border px-3 min-h-11 text-sm font-medium transition-colors",
                      categoryId === c.id
                        ? "bg-primary text-primary-foreground border-primary"
                        : "bg-background hover:bg-muted"
                    )}
                  >
                    {c.displayName}
                  </button>
                ))}
                {chips.length === 0 && (
                  <p className="text-sm text-muted-foreground py-2">No categories yet.</p>
                )}
              </div>
            </div>

            <Button
              className="w-full h-12 text-base"
              disabled={!canSave || submitting}
              onClick={handleSave}
            >
              {submitting ? "Saving..." : "Save expense"}
            </Button>
          </div>

          <DialogPrimitive.Close className="absolute right-4 top-4 rounded-sm opacity-70 transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring">
            <Cross2Icon className="h-4 w-4" />
            <span className="sr-only">Close</span>
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPortal>
    </Dialog>
  );
}
