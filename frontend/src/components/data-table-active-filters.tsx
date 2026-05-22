"use client";

import { Cross2Icon } from "@radix-ui/react-icons";
import { Table } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface DataTableActiveFiltersProps<TData> {
  table: Table<TData>;
}

export function DataTableActiveFilters<TData>({
  table,
}: DataTableActiveFiltersProps<TData>) {
  const filters = table.getState().columnFilters;
  const globalFilter = table.getState().globalFilter as string | undefined;

  const chips: { label: string; onRemove: () => void }[] = [];

  if (globalFilter) {
    chips.push({
      label: `Search: ${globalFilter}`,
      onRemove: () => table.setGlobalFilter(""),
    });
  }

  for (const filter of filters) {
    const column = table.getColumn(filter.id);
    if (!column) continue;
    const value = filter.value;

    if (filter.id === "date" && value && typeof value === "object") {
      const range = value as { from?: string; to?: string };
      if (range.from || range.to) {
        chips.push({
          label: `Date: ${range.from ?? "…"} → ${range.to ?? "…"}`,
          onRemove: () => column.setFilterValue(undefined),
        });
      }
      continue;
    }

    if (filter.id === "amount" && value && typeof value === "object") {
      const range = value as { min?: string; max?: string };
      if (range.min || range.max) {
        chips.push({
          label: `Amount: ${range.min ?? "0"} – ${range.max ?? "∞"}`,
          onRemove: () => column.setFilterValue(undefined),
        });
      }
      continue;
    }

    if (Array.isArray(value) && value.length > 0) {
      chips.push({
        label: `${filter.id}: ${value.join(", ")}`,
        onRemove: () => column.setFilterValue(undefined),
      });
      continue;
    }

    if (typeof value === "string" && value) {
      chips.push({
        label: `${filter.id}: ${value}`,
        onRemove: () => column.setFilterValue(""),
      });
    }
  }

  if (chips.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2">
      {chips.map((chip, index) => (
        <Badge key={`${chip.label}-${index}`} variant="secondary" className="gap-1 pr-1">
          <span className="max-w-[200px] truncate">{chip.label}</span>
          <button
            type="button"
            className="rounded-full p-0.5 hover:bg-muted"
            onClick={chip.onRemove}
            aria-label={`Remove filter ${chip.label}`}
          >
            <Cross2Icon className="h-3 w-3" />
          </button>
        </Badge>
      ))}
      <Button
        variant="ghost"
        size="sm"
        className="h-7 px-2 text-xs"
        onClick={() => {
          table.resetColumnFilters();
          table.setGlobalFilter("");
        }}
      >
        Clear all
      </Button>
    </div>
  );
}
