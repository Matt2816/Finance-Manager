"use client";

import { ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";

import { cardTypes, magnitudes, labels } from "./data/data";

import { DataTableColumnHeader } from "@/components/data-table-column-header";
import { DataTableRowActions } from "@/components/data-table-row-actions";
import { LocationMapPopover } from "@/components/location-map-popover";

import { Transaction } from "@/types/transaction";
import { parseAmount, getMagnitude } from "@/lib/transaction-analytics";

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
        }).format(parseAmount(String(row.getValue("amount"))))}
      </div>
    ),
    filterFn: (row, id, value) => {
      const range = value as { min?: string; max?: string } | undefined;
      if (!range?.min && !range?.max) return true;
      const amount = parseAmount(String(row.getValue(id)));
      const min = range.min ? Number.parseFloat(range.min) : undefined;
      const max = range.max ? Number.parseFloat(range.max) : undefined;
      if (min !== undefined && !Number.isNaN(min) && amount < min) return false;
      if (max !== undefined && !Number.isNaN(max) && amount > max) return false;
      return true;
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
      const date = new Date(row.getValue("date"));
      return <div>{date.toLocaleDateString()}</div>;
    },
    sortingFn: (rowA, rowB, columnId) => {
      const dateA = new Date(rowA.getValue(columnId));
      const dateB = new Date(rowB.getValue(columnId));
      return dateA.getTime() - dateB.getTime();
    },
    filterFn: (row, id, value) => {
      const range = value as { from?: string; to?: string } | undefined;
      if (!range?.from && !range?.to) return true;
      const date = new Date(row.getValue(id) as string);
      if (range.from) {
        const from = new Date(range.from);
        from.setHours(0, 0, 0, 0);
        if (date < from) return false;
      }
      if (range.to) {
        const to = new Date(range.to);
        to.setHours(23, 59, 59, 999);
        if (date > to) return false;
      }
      return true;
    },
  },
  {
    accessorKey: "city",
    id: "city",
    header: () => null,
    cell: () => null,
    enableSorting: false,
    enableHiding: true,
    filterFn: (row, id, value) => {
      const city = (row.getValue(id) as string) ?? "";
      return (value as string[]).includes(city);
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
      const amount = parseAmount(String(row.getValue("amount")));
      const magnitude = getMagnitude(amount);
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
      const amount = parseAmount(String(row.getValue("amount")));
      return (value as string[]).includes(getMagnitude(amount));
    },
  },
  {
    id: "actions",
    cell: ({ row }) => <DataTableRowActions row={row} />,
  },
];
