"use client";

import { useMemo, useState } from "react";
import { Cross2Icon } from "@radix-ui/react-icons";
import { Table } from "@tanstack/react-table";
import { SlidersHorizontal } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DataTableViewOptions } from "@/components/data-table-view-options";
import { DataTableActiveFilters } from "@/components/data-table-active-filters";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

import { cardTypes, magnitudes } from "@/components/data/data";
import { DataTableFacetedFilter } from "@/components/data-table-faceted-filter";
import { Transaction } from "@/types/transaction";

interface DataTableToolbarProps<TData> {
  table: Table<TData>;
}

function AdvancedFilters<TData>({ table }: { table: Table<TData> }) {
  const dateFilter =
    (table.getColumn("date")?.getFilterValue() as {
      from?: string;
      to?: string;
    }) ?? {};
  const amountFilter =
    (table.getColumn("amount")?.getFilterValue() as {
      min?: string;
      max?: string;
    }) ?? {};

  const cityOptions = useMemo(() => {
    const cities = new Set<string>();
    for (const row of table.getPreFilteredRowModel().rows) {
      const tx = row.original as Transaction;
      if (tx.city) cities.add(tx.city);
    }
    return [...cities]
      .sort()
      .map((city) => ({ label: city, value: city }));
  }, [table]);

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="filter-from" className="text-xs">
            From date
          </Label>
          <Input
            id="filter-from"
            type="date"
            value={dateFilter.from ?? ""}
            onChange={(e) =>
              table.getColumn("date")?.setFilterValue({
                ...dateFilter,
                from: e.target.value || undefined,
              })
            }
            className="h-9"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filter-to" className="text-xs">
            To date
          </Label>
          <Input
            id="filter-to"
            type="date"
            value={dateFilter.to ?? ""}
            onChange={(e) =>
              table.getColumn("date")?.setFilterValue({
                ...dateFilter,
                to: e.target.value || undefined,
              })
            }
            className="h-9"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filter-min" className="text-xs">
            Min amount ($)
          </Label>
          <Input
            id="filter-min"
            type="number"
            min={0}
            placeholder="0"
            value={amountFilter.min ?? ""}
            onChange={(e) =>
              table.getColumn("amount")?.setFilterValue({
                ...amountFilter,
                min: e.target.value || undefined,
              })
            }
            className="h-9"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filter-max" className="text-xs">
            Max amount ($)
          </Label>
          <Input
            id="filter-max"
            type="number"
            min={0}
            placeholder="Any"
            value={amountFilter.max ?? ""}
            onChange={(e) =>
              table.getColumn("amount")?.setFilterValue({
                ...amountFilter,
                max: e.target.value || undefined,
              })
            }
            className="h-9"
          />
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        {table.getColumn("cardType") && (
          <DataTableFacetedFilter
            column={table.getColumn("cardType")}
            title="Card"
            options={cardTypes}
          />
        )}
        {table.getColumn("magnitude") && (
          <DataTableFacetedFilter
            column={table.getColumn("magnitude")}
            title="Size"
            options={magnitudes}
          />
        )}
        {table.getColumn("city") && cityOptions.length > 0 && (
          <DataTableFacetedFilter
            column={table.getColumn("city")}
            title="City"
            options={cityOptions}
          />
        )}
      </div>
    </div>
  );
}

export function DataTableToolbar<TData>({
  table,
}: DataTableToolbarProps<TData>) {
  const [filtersOpen, setFiltersOpen] = useState(false);
  const isFiltered =
    table.getState().columnFilters.length > 0 ||
    Boolean(table.getState().globalFilter);

  const activeFilterCount =
    table.getState().columnFilters.length +
    (table.getState().globalFilter ? 1 : 0);

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="flex flex-1 flex-col gap-3">
          <Input
            placeholder="Search name, merchant, location, city…"
            value={(table.getState().globalFilter as string) ?? ""}
            onChange={(e) => table.setGlobalFilter(e.target.value)}
            className="h-9 w-full"
          />
          <div className="hidden lg:block">
            <AdvancedFilters table={table} />
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          <Dialog open={filtersOpen} onOpenChange={setFiltersOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" size="sm" className="h-9 lg:hidden">
                <SlidersHorizontal className="mr-2 h-4 w-4" />
                Filters
                {activeFilterCount > 0 && (
                  <span className="ml-2 rounded-full bg-primary px-1.5 py-0.5 text-xs text-primary-foreground">
                    {activeFilterCount}
                  </span>
                )}
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Filter transactions</DialogTitle>
              </DialogHeader>
              <AdvancedFilters table={table} />
              <Button className="w-full" onClick={() => setFiltersOpen(false)}>
                Done
              </Button>
            </DialogContent>
          </Dialog>

          {isFiltered && (
            <Button
              variant="ghost"
              onClick={() => {
                table.resetColumnFilters();
                table.setGlobalFilter("");
              }}
              className="h-9 px-2"
            >
              Reset
              <Cross2Icon className="ml-2 h-4 w-4" />
            </Button>
          )}
          <DataTableViewOptions table={table} />
        </div>
      </div>

      <DataTableActiveFilters table={table} />
    </div>
  );
}
