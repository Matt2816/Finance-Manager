"use client";

import { useState, useEffect } from "react";
import { Transaction } from "@/types/transaction";
import { Category } from "@/hooks/use-categories";
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
import { Plus } from "lucide-react";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch } from "@/lib/authenticated-fetch";

const cardTypeOptions = [
  { value: "visa", label: "Visa" },
  { value: "mastercard", label: "Mastercard" },
  { value: "amex", label: "Amex" },
  { value: "debit", label: "Debit" },
  { value: "other", label: "Other" },
];

interface TransactionFormDialogProps {
  mode: "add" | "edit";
  transaction?: Transaction | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
  categories?: Category[];
}

function toLocalISODate(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDateForInput(dateStr: string): string {
  try {
    return toLocalISODate(new Date(dateStr));
  } catch {
    return "";
  }
}

function generateHash(name: string, merchant: string, amount: string, date: string) {
  const timestamp = Date.now().toString(36);
  return `${name}-${merchant}-${amount}-${date}-${timestamp}`;
}

export function TransactionFormDialog({
  mode,
  transaction,
  open,
  onOpenChange,
  onSuccess,
  categories,
}: TransactionFormDialogProps) {
  const [name, setName] = useState("");
  const [merchant, setMerchant] = useState("");
  const [amount, setAmount] = useState("");
  const [cardType, setCardType] = useState("other");
  const [transactionDate, setTransactionDate] = useState("");
  const [categoryId, setCategoryId] = useState<string>("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open && mode === "edit" && transaction) {
      setName(transaction.name ?? "");
      setMerchant(transaction.merchant ?? "");
      setAmount(transaction.amount ?? "");
      setCardType(transaction.cardType ?? "other");
      setTransactionDate(formatDateForInput(transaction.transactionDate));
      // address removed from form, defaults to empty
      setCategoryId(transaction.categoryId?.toString() ?? "");
      setError(null);
    } else if (open && mode === "add") {
      setName("");
      setMerchant("");
      setAmount("");
      setCardType("other");
      setTransactionDate(toLocalISODate(new Date()));
      // address defaults to empty for manually added transactions
      setCategoryId("");
      setError(null);
    }
  }, [open, mode, transaction]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    const cleanAmount = amount.trim();
    if (!name.trim() || !cleanAmount || !transactionDate) {
      setError("Name, amount, and date are required.");
      setSubmitting(false);
      return;
    }

    const payload: any = {
      cardType,
      amount: cleanAmount,
      name: name.trim(),
      merchant: merchant.trim(),
      transactionDate,
      hash:
        mode === "edit" && transaction
          ? transaction.hash
          : generateHash(name.trim(), merchant.trim(), cleanAmount, transactionDate),
      address: "",
    };
    if (categoryId) {
      payload.categoryId = Number(categoryId);
    }

    try {
      const url =
        mode === "edit" && transaction
          ? `${getApiBaseUrl()}/api/transaction/${transaction.id}`
          : `${getApiBaseUrl()}/api/transaction`;
      const method = mode === "edit" ? "PUT" : "POST";

      const res = await authenticatedFetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || `Failed to ${mode} transaction`);
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
            {mode === "add" ? "Add Transaction" : "Edit Transaction"}
          </DialogTitle>
          <DialogDescription>
            {mode === "add"
              ? "Create a new transaction entry."
              : "Update the transaction details."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-3 sm:space-y-4">
          <div className="grid grid-cols-1 gap-4">
            <div className="space-y-2">
              <Label htmlFor="tx-name">Name</Label>
              <Input
                id="tx-name"
                placeholder="Transaction name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="tx-merchant">Merchant</Label>
              <Input
                id="tx-merchant"
                placeholder="Merchant or vendor"
                value={merchant}
                onChange={(e) => setMerchant(e.target.value)}
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 min-w-0 overflow-hidden">
              <div className="space-y-2 min-w-0">
                <Label htmlFor="tx-amount">Amount</Label>
                <Input
                  id="tx-amount"
                  placeholder="$25.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  className="max-w-full"
                />
              </div>
              <div className="space-y-2 min-w-0">
                <Label htmlFor="tx-date">Date</Label>
                <Input
                  id="tx-date"
                  type="date"
                  value={transactionDate}
                  onChange={(e) => setTransactionDate(e.target.value)}
                  required
                  className="min-w-0 max-w-full"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="tx-card">Card Type</Label>
                <Select value={cardType} onValueChange={setCardType}>
                  <SelectTrigger id="tx-card">
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
              <div className="space-y-2">
                <Label htmlFor="tx-category">Category</Label>
                <Select value={categoryId} onValueChange={setCategoryId}>
                  <SelectTrigger id="tx-category">
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories
                      ?.filter((c) => c.slug !== "uncategorized")
                      .map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.displayName}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
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
                ? mode === "add"
                  ? "Adding..."
                  : "Saving..."
                : mode === "add"
                  ? "Add Transaction"
                  : "Save Changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function AddTransactionButton({ onClick }: { onClick: () => void }) {
  return (
    <Button onClick={onClick} className="gap-2">
      <Plus className="h-4 w-4" />
      <span className="hidden sm:inline">Add Transaction</span>
      <span className="sm:hidden">Add</span>
    </Button>
  );
}
