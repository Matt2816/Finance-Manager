export interface WalletNotePayload {
  name: string;
  merchant: string;
  amount: string;
  date: string;
  location: string;
  categoryId?: number;
}

function toLocalISODate(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function normalizeAmount(raw: string): string {
  return raw.replace(/[^0-9.\-]/g, "").trim();
}

export interface QuickAddInput {
  amount: string;
  categoryId: number;
  categoryLabel: string;
  merchant?: string;
  date?: Date;
}

export function buildQuickAddPayload(input: QuickAddInput): WalletNotePayload {
  return {
    name: input.categoryLabel || "Cash expense",
    merchant: input.merchant?.trim() || "Cash",
    amount: normalizeAmount(input.amount),
    date: toLocalISODate(input.date ?? new Date()),
    location: "",
    categoryId: input.categoryId,
  };
}
