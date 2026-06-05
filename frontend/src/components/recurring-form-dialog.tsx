"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  createRecurring,
  updateRecurring,
} from "@/hooks/use-recurring";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedJson } from "@/lib/authenticated-fetch";

const frequencyOptions = [
  { value: "WEEKLY", label: "Weekly" },
  { value: "BIWEEKLY", label: "Bi-Weekly" },
  { value: "MONTHLY", label: "Monthly" },
];

const cardTypeOptions = [
  { value: "visa", label: "Visa" },
  { value: "mastercard", label: "Mastercard" },
  { value: "amex", label: "Amex" },
  { value: "debit", label: "Debit" },
  { value: "other", label: "Other" },
];

interface RecurringFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialData?: any;
  onSuccess: () => void;
}

export function RecurringFormDialog({
  open,
  onOpenChange,
  initialData,
  onSuccess,
}: RecurringFormDialogProps) {
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [direction, setDirection] = useState<"DEBIT" | "CREDIT">("DEBIT");
  const [frequency, setFrequency] = useState<"WEEKLY" | "BIWEEKLY" | "MONTHLY">("MONTHLY");
  const [startDate, setStartDate] = useState("");
  const [hasEndDate, setHasEndDate] = useState(false);
  const [endDate, setEndDate] = useState("");
  const [cardType, setCardType] = useState("other");
  const [categoryId, setCategoryId] = useState<string>("");
  const [categories, setCategories] = useState<{ id: number; displayName: string }[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isEdit = !!initialData;

  useEffect(() => {
    authenticatedJson<{ id: number; displayName: string; slug: string }[]>(
      `${getApiBaseUrl()}/api/categories`
    )
      .then((data) => setCategories(data.filter((c) => c.slug !== "uncategorized")));
  }, []);

  useEffect(() => {
    if (open && initialData) {
      setName(initialData.name ?? "");
      setAmount(String(initialData.amount ?? ""));
      setDirection(initialData.direction ?? "DEBIT");
      setFrequency(initialData.frequency ?? "MONTHLY");
      setStartDate(initialData.startDate ?? "");
      setHasEndDate(!!initialData.endDate);
      setEndDate(initialData.endDate ?? "");
      setCardType(initialData.cardType ?? "other");
      setCategoryId(initialData.categoryId?.toString() ?? "");
      setError(null);
    } else if (open) {
      setName("");
      setAmount("");
      setDirection("DEBIT");
      setFrequency("MONTHLY");
      setStartDate(new Date().toISOString().split("T")[0]);
      setHasEndDate(false);
      setEndDate("");
      setCardType("other");
      setCategoryId("");
      setError(null);
    }
  }, [open, initialData]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    const cleanAmount = String(amount).trim();
    if (!name.trim() || !cleanAmount || !startDate) {
      setError("Name, amount, and start date are required.");
      setSubmitting(false);
      return;
    }

    const payload = {
      name: name.trim(),
      amount: cleanAmount,
      direction,
      frequency,
      startDate,
      endDate: hasEndDate ? endDate : null,
      cardType,
      categoryId: categoryId ? Number(categoryId) : null,
    };

    try {
      if (isEdit) {
        await updateRecurring(initialData.id, payload);
      } else {
        await createRecurring(payload);
      }
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      setError(err.message || "Something went wrong.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Edit Recurring" : "Add Recurring"}
          </DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Update this recurring transaction series."
              : "Create a new recurring income or expense."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-3 sm:space-y-4">
          <div className="grid grid-cols-1 gap-4">
            <div className="space-y-2">
              <Label htmlFor="rec-direction">Type</Label>
              <Select value={direction} onValueChange={(v) => setDirection(v as "DEBIT" | "CREDIT")}>
                <SelectTrigger id="rec-direction">
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DEBIT">Expense (money out)</SelectItem>
                  <SelectItem value="CREDIT">Income (money in)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="rec-name">Name</Label>
              <Input
                id="rec-name"
                placeholder="e.g., Rent, Salary, Gym"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="rec-amount">Amount</Label>
                <Input
                  id="rec-amount"
                  placeholder="$100.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="rec-freq">Frequency</Label>
                <Select value={frequency} onValueChange={(v) => setFrequency(v as "WEEKLY" | "BIWEEKLY" | "MONTHLY")}>
                  <SelectTrigger id="rec-freq">
                    <SelectValue placeholder="Select frequency" />
                  </SelectTrigger>
                  <SelectContent>
                    {frequencyOptions.map((f) => (
                      <SelectItem key={f.value} value={f.value}>
                        {f.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2 min-w-0">
                <Label htmlFor="rec-start">First Date</Label>
                <Input
                  id="rec-start"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                  className="min-w-0"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="rec-card">Card Type</Label>
                <Select value={cardType} onValueChange={setCardType}>
                  <SelectTrigger id="rec-card">
                    <SelectValue placeholder="Select card" />
                  </SelectTrigger>
                  <SelectContent>
                    {cardTypeOptions.map((c) => (
                      <SelectItem key={c.value} value={c.value}>
                        {c.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="rec-category">Category</Label>
              <Select value={categoryId} onValueChange={setCategoryId}>
                <SelectTrigger id="rec-category">
                  <SelectValue placeholder="Select category (optional)" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.displayName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center gap-3">
              <input
                id="rec-end-toggle"
                type="checkbox"
                checked={hasEndDate}
                onChange={(e) => setHasEndDate(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              <Label htmlFor="rec-end-toggle" className="text-sm cursor-pointer">
                Set end date
              </Label>
            </div>
            {hasEndDate && (
              <div className="space-y-2 min-w-0">
                <Label htmlFor="rec-end">End Date</Label>
                <Input
                  id="rec-end"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="min-w-0"
                />
              </div>
            )}
          </div>

          {error && (
            <p className="text-sm text-red-600">{error}</p>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting
                ? isEdit
                  ? "Saving..."
                  : "Adding..."
                : isEdit
                  ? "Save Changes"
                  : "Add Recurring"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
