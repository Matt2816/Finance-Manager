import { z } from "zod";

// We're keeping a simple non-relational schema here.
// IRL, you will have a schema for your data models.
export const transactionSchema = z.object({
  id: z.number(),
  label: z.enum(["recurringIncome", "recurringExpense", "transaction"]),
  cardType: z.enum(["amex", "debit", "mastercard", "visa", "other"]),
  amount: z.string(),
  name: z.string(),
  merchant: z.string(),
  transactionDate: z.string(),
  address: z.string(),
  hash: z.string(),
});
