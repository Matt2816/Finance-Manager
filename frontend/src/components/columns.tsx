"use client";

import { ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";

import { cardTypes, magnitudes, labels } from "./data/data";

import { DataTableColumnHeader } from "@/components/data-table-column-header";
import { DataTableRowActions } from "@/components/data-table-row-actions";

import { Transaction } from "@/types/transaction";

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
    cell: ({ row }) => (
      <div className="w-[80px]">
        {new Intl.NumberFormat("en-CA", {
          style: "currency",
          currency: "CAD",
        }).format(row.getValue("amount"))}
      </div>
    ),
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
    accessorKey: "transactionDate",
    id: "date",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Date" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("date"));
      return <div>{date.toLocaleDateString()}</div>;
    },
    sortingFn: (rowA, rowB, columnId) => {
      const dateA = new Date(rowA.getValue(columnId));
      const dateB = new Date(rowB.getValue(columnId));
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
    id: "actions",
    cell: ({ row }) => <DataTableRowActions row={row} />,
  },
];
