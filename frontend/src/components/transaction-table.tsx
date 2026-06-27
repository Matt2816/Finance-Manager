"use client";

import * as React from "react";
import {
  ColumnDef,
  ColumnFiltersState,
  SortingState,
  VisibilityState,
  flexRender,
  getCoreRowModel,
  getFacetedRowModel,
  getFacetedUniqueValues,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

import { DataTableToolbar } from "@/components/data-table-toolbar";
import { DataTablePagination } from "./data-table-pagination";

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  onEdit?: (tx: any) => void;
  onDelete?: (tx: any) => void;
  onCategorize?: (tx: any, categoryId: number) => void;
  categories?: { id: number; displayName: string }[];
}

export function TransactionTable<TData, TValue>({
  columns,
  data,
  onEdit,
  onDelete,
  onCategorize,
  categories,
}: DataTableProps<TData, TValue>) {
  const [rowSelection, setRowSelection] = React.useState({});
  const [columnVisibility, setColumnVisibility] =
    React.useState<VisibilityState>({
      cardType: false,
      address: false,
      magnitude: false,
    });
  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>(
    []
  );
  const [sorting, setSorting] = React.useState<SortingState>([
    { id: "date", desc: true },
  ]);

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      columnVisibility,
      rowSelection,
      columnFilters,
    },
    meta: { onEdit, onDelete, onCategorize, categories } as any,
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnVisibilityChange: setColumnVisibility,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
  });

  const rows = table.getRowModel().rows;

  return (
    <div className="space-y-4">
      <DataTableToolbar table={table} categories={categories} />

      {/* Mobile card view */}
      <div className="sm:hidden space-y-2">
        {rows?.length ? (
          rows.map((row) => {
            const cellsById = Object.fromEntries(
              row.getVisibleCells().map((c) => [c.column.id, c])
            );
            return (
              <div
                key={row.id}
                className="rounded-md border p-3 bg-card shadow-sm space-y-2"
                data-state={row.getIsSelected() && "selected"}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0 flex-1 text-sm font-medium">
                    {cellsById.name
                      ? flexRender(
                          cellsById.name.column.columnDef.cell,
                          cellsById.name.getContext()
                        )
                      : null}
                  </div>
                  <div className="shrink-0 text-sm">
                    {cellsById.amount
                      ? flexRender(
                          cellsById.amount.column.columnDef.cell,
                          cellsById.amount.getContext()
                        )
                      : null}
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                  {cellsById.date
                    ? flexRender(
                        cellsById.date.column.columnDef.cell,
                        cellsById.date.getContext()
                      )
                    : null}
                  {cellsById.category
                    ? flexRender(
                        cellsById.category.column.columnDef.cell,
                        cellsById.category.getContext()
                      )
                    : null}
                  {cellsById.label
                    ? flexRender(
                        cellsById.label.column.columnDef.cell,
                        cellsById.label.getContext()
                      )
                    : null}
                  {cellsById.cardType
                    ? flexRender(
                        cellsById.cardType.column.columnDef.cell,
                        cellsById.cardType.getContext()
                      )
                    : null}
                </div>
                <div className="flex items-center justify-between pt-1 border-t">
                  <div>
                    {cellsById.select
                      ? flexRender(
                          cellsById.select.column.columnDef.cell,
                          cellsById.select.getContext()
                        )
                      : null}
                  </div>
                  <div>
                    {cellsById.actions
                      ? flexRender(
                          cellsById.actions.column.columnDef.cell,
                          cellsById.actions.getContext()
                        )
                      : null}
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="rounded-md border p-8 text-center text-sm text-muted-foreground">
            No results.
          </div>
        )}
      </div>

      {/* Desktop table view */}
      <div className="hidden sm:block rounded-md border overflow-x-auto">
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  return (
                    <TableHead key={header.id} colSpan={header.colSpan}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                    </TableHead>
                  );
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {rows?.length ? (
              rows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className="h-24 text-center"
                >
                  No results.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <DataTablePagination table={table} />
    </div>
  );
}
