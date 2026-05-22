"use client";

import { Row } from "@tanstack/react-table";

import { Transaction } from "@/types/transaction";
import { cardTypes } from "@/components/data/data";
import { LocationMapPopover } from "@/components/location-map-popover";
import { parseAmount } from "@/lib/transaction-analytics";

interface TransactionMobileCardsProps {
  rows: Row<Transaction>[];
}

export function TransactionMobileCards({ rows }: TransactionMobileCardsProps) {
  if (!rows.length) {
    return (
      <p className="py-8 text-center text-sm text-muted-foreground">
        No transactions match your filters.
      </p>
    );
  }

  return (
    <ul className="divide-y rounded-md border md:hidden">
      {rows.map((row) => {
        const tx = row.original;
        const amount = parseAmount(tx.amount);
        const card = cardTypes.find((c) => c.value === tx.cardType);
        const date = new Date(tx.transactionDate);

        return (
          <li key={row.id} className="flex flex-col gap-2 p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 items-center gap-2">
                {card?.icon && <card.icon className="h-7 w-7 shrink-0" />}
                <div className="min-w-0">
                  <p className="truncate font-medium">{tx.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {date.toLocaleDateString("en-CA", {
                      weekday: "short",
                      month: "short",
                      day: "numeric",
                    })}
                  </p>
                </div>
              </div>
              <p className="shrink-0 text-lg font-semibold tabular-nums">
                {new Intl.NumberFormat("en-CA", {
                  style: "currency",
                  currency: "CAD",
                }).format(amount)}
              </p>
            </div>
            {tx.address?.trim() ? (
              <LocationMapPopover
                address={tx.address}
                className="max-w-full px-0"
              />
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}
