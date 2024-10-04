import {
  ArrowDownIcon,
  ArrowRightIcon,
  ArrowUpIcon,
} from "@radix-ui/react-icons";
import {
  VisaIcon,
  MastercardIcon,
  AmexIcon,
  DebitIcon,
  OtherIcon,
} from "../icons";

export const labels = [
  {
    value: "recurringIncome",
    label: "Recurring Income",
  },
  {
    value: "transaction",
    label: "Transaction",
  },
  {
    value: "recurringExpense",
    label: "Recurring Expense",
  },
];

export const cardTypes = [
  {
    value: "visa",
    label: "Visa",
    icon: VisaIcon,
  },
  {
    value: "mastercard",
    label: "Mastercard",
    icon: MastercardIcon,
  },
  {
    value: "amex",
    label: "Amex",
    icon: AmexIcon,
  },
  {
    value: "debit",
    label: "Debit",
    icon: DebitIcon,
  },
  {
    value: "other",
    label: "Other",
    icon: OtherIcon,
  },
];

export const magnitudes = [
  {
    label: "Low",
    value: "low",
    icon: ArrowDownIcon,
  },
  {
    label: "Medium",
    value: "medium",
    icon: ArrowRightIcon,
  },
  {
    label: "High",
    value: "high",
    icon: ArrowUpIcon,
  },
];
