"use client";

import { ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";

import { cardTypes, magnitudes, labels } from "./data/data";

import { DataTableColumnHeader } from "@/components/data-table-column-header";
import { DataTableRowActions } from "@/components/data-table-row-actions";
import { LocationMapPopover } from "@/components/location-map-popover";

import { Transaction } from "@/types/transaction";
import { ArrowDownLeft, ArrowUpRight, Repeat } from "lucide-react";

export const columns: ColumnDef<Transaction>[] = [
  {
    id: "select",
    header: ({ table }) => (
      <Checkbox
        checked={
          table.getIsAllPageRowsSelected() ||
          (table.getIsSomePageRowsSelected() && "indeterminate")
        }
        onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
        aria-label="Select all"
        className="translate-y-[2px]"
      />
    ),
    cell: ({ row }) => (
      <Checkbox
        checked={row.getIsSelected()}
        onCheckedChange={(value) => row.toggleSelected(!!value)}
        aria-label="Select row"
        className="translate-y-[2px]"
      />
    ),
    enableSorting: false,
    enableHiding: false,
  },
  {
    accessorKey: "amount",
    id: "amount",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Amount" />
    ),
    cell: ({ row }) => {
      const amountStr = row.getValue("amount") as string;
      const amountNum = parseFloat(amountStr.replace("$", ""));
      const isIncome = amountNum < 0;
      const displayAmount = Math.abs(amountNum);
      const isRecurring = !!row.original.recurringParentId;

      return (
        <div className="flex items-center gap-1.5 w-[110px]">
          {isIncome ? (
            <ArrowDownLeft className="h-3.5 w-3.5 text-green-600 shrink-0" />
          ) : (
            <ArrowUpRight className="h-3.5 w-3.5 text-red-600 shrink-0" />
          )}
          <span className={isIncome ? "text-green-600" : "text-red-600"}>
            {new Intl.NumberFormat("en-CA", {
              style: "currency",
              currency: "CAD",
            }).format(displayAmount)}
          </span>
          {isRecurring && (
            <span title="Recurring">
              <Repeat className="h-3 w-3 text-muted-foreground shrink-0" />
            </span>
          )}
        </div>
      );
    },
  },
  {
    accessorKey: "name",
    id: "name",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Name" />
    ),
    cell: ({ row }) => {
      const cardType = cardTypes.find(
        (cardType) => cardType.value === row.original.cardType
      );

      return (
        <div className="flex space-x-2 items-center">
          {cardType && <cardType.icon className="h-8 w-8" />}
          <span className="max-w-[500px] truncate font-medium">
            {row.getValue("name")}
          </span>
        </div>
      );
    },
    filterFn: (row, id, value) => {
      const name = row.getValue("name") as string;
      const cardType = row.original.cardType as string;
      const searchValue = value.toLowerCase();
      return (
        name.toLowerCase().includes(searchValue) ||
        cardType.toLowerCase().includes(searchValue)
      );
    },
  },
  {
    accessorKey: "address",
    id: "address",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Location" />
    ),
    cell: ({ row }) => (
      <LocationMapPopover address={row.original.address ?? ""} />
    ),
    filterFn: (row, id, value) => {
      const address = (row.getValue(id) as string) ?? "";
      return address.toLowerCase().includes(value.toLowerCase());
    },
  },
  {
    accessorKey: "transactionDate",
    id: "date",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Date" />
    ),
    cell: ({ row }) => {
      const raw = row.getValue("date") as string;
      const date = new Date(raw + "T00:00:00");
      return <div>{date.toLocaleDateString()}</div>;
    },
    sortingFn: (rowA, rowB, columnId) => {
      const rawA = rowA.getValue(columnId) as string;
      const rawB = rowB.getValue(columnId) as string;
      const dateA = new Date(rawA + "T00:00:00");
      const dateB = new Date(rawB + "T00:00:00");
      return dateA.getTime() - dateB.getTime();
    },
  },
  {
    accessorKey: "cardType",
    id: "cardType",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Card Type" />
    ),
    filterFn: (row, id, value) => {
      return value.includes(row.getValue(id));
    },
    enableHiding: false,
  },
  {
    accessorKey: "label",
    id: "label",
    enableSorting: false,
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Label" />
    ),
    cell: ({ row }) => {
      const label = labels.find(
        (label) => label.value === row.getValue("label")
      );

      if (!label) {
        return null;
      }

      return (
        <div className="flex w-[100px] items-center">
          {label && <Badge variant="outline">{label.label}</Badge>}
        </div>
      );
    },
    filterFn: (row, id, value) => {
      return value.includes(row.getValue(id));
    },
  },
  {
    accessorKey: "magnitude",
    id: "magnitude",
    enableSorting: false,
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Magnitude" />
    ),
    cell: ({ row }) => {
      const amount = parseFloat(row.getValue("amount"));
      let magnitude;
      if (amount > 100) {
        magnitude = "high";
      } else if (amount > 20) {
        magnitude = "medium";
      } else {
        magnitude = "low";
      }
      const priority = magnitudes.find(
        (priority) => priority.value === magnitude
      );

      if (!priority) {
        return null;
      }

      return (
        <div className="flex items-center">
          {priority.icon && (
            <priority.icon className="mr-2 h-4 w-4 text-muted-foreground" />
          )}
          <span>{priority.label}</span>
        </div>
      );
    },
    filterFn: (row, id, value) => {
      const amount = parseFloat(row.getValue("amount"));
      let magnitude;
      if (amount > 100) {
        magnitude = "high";
      } else if (amount > 20) {
        magnitude = "medium";
      } else {
        magnitude = "low";
      }
      return value.includes(magnitude);
    },
  },
  {
    accessorKey: "categoryId",
    id: "category",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Category" />
    ),
    cell: ({ row, table }) => {
      const meta = table.options.meta as any;
      const categories: { id: number; displayName: string }[] = meta?.categories ?? [];
      const categoryId = row.getValue("category") as number | null;
      const category = categories.find((c) => c.id === categoryId);
      return (
        <div className="flex w-[100px] items-center">
          {category ? (
            <Badge variant="secondary">{category.displayName}</Badge>
          ) : (
            <Badge variant="outline" className="text-muted-foreground">—</Badge>
          )}
        </div>
      );
    },
    enableSorting: false,
  },
  {
    id: "actions",
    cell: ({ row, table }) => {
      const meta = table.options.meta as any;
      return (
        <DataTableRowActions
          row={row}
          onEdit={meta?.onEdit}
          onDelete={meta?.onDelete}
          onCategorize={meta?.onCategorize}
          categories={meta?.categories}
        />
      );
    },
  },
];
