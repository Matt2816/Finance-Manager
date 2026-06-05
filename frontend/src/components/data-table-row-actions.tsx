"use client";

import { DotsHorizontalIcon } from "@radix-ui/react-icons";
import { Row } from "@tanstack/react-table";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { labels } from "@/components/data/data";
import { transactionSchema } from "@/components/data/schema";

interface DataTableRowActionsProps<TData> {
  row: Row<TData>;
  onEdit?: (tx: any) => void;
  onDelete?: (tx: any) => void;
  onCategorize?: (tx: any, categoryId: number) => void;
  categories?: { id: number; displayName: string }[];
}

export function DataTableRowActions<TData>({
  row,
  onEdit,
  onDelete,
  onCategorize,
  categories,
}: DataTableRowActionsProps<TData>) {
  const transaction = transactionSchema.parse(row.original);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
        >
          <DotsHorizontalIcon className="h-4 w-4" />
          <span className="sr-only">Open menu</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-[180px]">
        <DropdownMenuItem onClick={() => onEdit?.(transaction)}>
          Edit
        </DropdownMenuItem>
        <DropdownMenuItem>Favorite</DropdownMenuItem>
        <DropdownMenuSeparator />
        {categories && categories.length > 0 && onCategorize && (
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>Categorize</DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup
                value={transaction.categoryId?.toString() ?? ""}
              >
                {categories.map((cat) => (
                  <DropdownMenuRadioItem
                    key={cat.id}
                    value={cat.id.toString()}
                    onClick={() => onCategorize(transaction, cat.id)}
                  >
                    {cat.displayName}
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
        )}
        <DropdownMenuSub>
          <DropdownMenuSubTrigger>Labels</DropdownMenuSubTrigger>
          <DropdownMenuSubContent>
            <DropdownMenuRadioGroup value={transaction.label}>
              {labels.map((label) => (
                <DropdownMenuRadioItem key={label.value} value={label.value}>
                  {label.label}
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </DropdownMenuSubContent>
        </DropdownMenuSub>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={() => onDelete?.(transaction)}
          className="text-red-600 focus:text-red-600"
        >
          Delete
          <DropdownMenuShortcut>⌘⌫</DropdownMenuShortcut>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
