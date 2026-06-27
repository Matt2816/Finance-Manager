"use client";

import * as React from "react";
import { Cross2Icon } from "@radix-ui/react-icons";
import { Table } from "@tanstack/react-table";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DataTableViewOptions } from "@/components/data-table-view-options";

import { cardTypes } from "@/components/data/data";
import { DataTableFacetedFilter } from "@/components/data-table-faceted-filter";

interface DataTableToolbarProps<TData> {
  table: Table<TData>;
  categories?: { id: number; displayName: string }[];
}

export function DataTableToolbar<TData>({
  table,
  categories = [],
}: DataTableToolbarProps<TData>) {
  const isFiltered = table.getState().columnFilters.length > 0;
  const categoryOptions = React.useMemo(
    () =>
      categories
        .map((category) => ({
          label: category.displayName,
          value: String(category.id),
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    [categories]
  );

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-1 flex-wrap items-center gap-2">
        <Input
          placeholder="Filter by name"
          value={(table.getColumn("name")?.getFilterValue() as string) ?? ""}
          onChange={(event) =>
            table.getColumn("name")?.setFilterValue(event.target.value)
          }
          className="h-8 w-full sm:w-[150px] lg:w-[200px]"
        />
        {table.getColumn("cardType") && (
          <DataTableFacetedFilter
            column={table.getColumn("cardType")}
            title="Card Type"
            options={cardTypes}
          />
        )}
        {table.getColumn("category") && categoryOptions.length > 0 && (
          <DataTableFacetedFilter
            column={table.getColumn("category")}
            title="Category"
            options={categoryOptions}
          />
        )}
        {isFiltered && (
          <Button
            variant="ghost"
            onClick={() => table.resetColumnFilters()}
            className="h-8 px-2 lg:px-3"
          >
            Reset
            <Cross2Icon className="ml-2 h-4 w-4" />
          </Button>
        )}
      </div>
      <div className="shrink-0">
        <DataTableViewOptions table={table} />
      </div>
    </div>
  );
}
