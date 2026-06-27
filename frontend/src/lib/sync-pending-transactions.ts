import { mutate as globalMutate } from "swr";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch } from "@/lib/authenticated-fetch";
import {
  listPending,
  removePending,
  markAttempt,
  countPending,
  type PendingTransaction,
} from "@/lib/offline-queue";
import {
  showSyncSystemNotification,
  setPendingBadge,
} from "@/lib/sync-notifications";

export const PENDING_CHANGED_EVENT = "pending-sync-changed";
export const SYNC_STATE_EVENT = "pending-sync-state";

interface BatchItemResult {
  index: number;
  status: string;
  message: string;
  hash: string | null;
}

interface BatchResult {
  total: number;
  created: number;
  duplicates: number;
  skipped: number;
  results: BatchItemResult[];
}

export interface SyncOutcome {
  created: number;
  duplicates: number;
  remaining: number;
  ranOffline?: boolean;
}

let syncInProgress = false;

function emitPendingChanged() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(PENDING_CHANGED_EVENT));
  }
}

function emitSyncState(active: boolean) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(SYNC_STATE_EVENT, { detail: { active } }));
  }
}

async function refreshBadge() {
  const count = await countPending();
  setPendingBadge(count);
}

type ToastFn = (message: string, type?: "success" | "error") => void;

/**
 * Drain the offline queue to the backend batch endpoint. Safe to call from any
 * trigger; a mutex prevents overlapping runs. Items that are created or already
 * exist (duplicate) are removed from the queue. Network failures keep items for
 * the next automatic attempt.
 */
export async function syncPendingTransactions(onToast?: ToastFn): Promise<SyncOutcome> {
  if (syncInProgress) return { created: 0, duplicates: 0, remaining: await countPending() };
  if (typeof navigator !== "undefined" && !navigator.onLine) {
    return { created: 0, duplicates: 0, remaining: await countPending(), ranOffline: true };
  }

  const items = await listPending();
  if (items.length === 0) {
    await refreshBadge();
    return { created: 0, duplicates: 0, remaining: 0 };
  }

  syncInProgress = true;
  emitSyncState(true);

  try {
    const byIndex = new Map<number, PendingTransaction>();
    items.forEach((item, i) => byIndex.set(i, item));

    const res = await authenticatedFetch(
      `${getApiBaseUrl()}/api/transaction/import/wallet-notes-batch`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(items.map((it) => it.payload)),
      }
    );

    if (!res.ok) {
      throw new Error(`Batch sync failed: ${res.status}`);
    }

    const result = (await res.json()) as BatchResult;

    for (const itemResult of result.results) {
      const pending = byIndex.get(itemResult.index);
      if (!pending) continue;
      if (itemResult.status === "created" || itemResult.status === "duplicate") {
        await removePending(pending.id);
      } else {
        await markAttempt(pending, itemResult.message);
      }
    }

    const remaining = await countPending();
    await refreshBadge();
    emitPendingChanged();

    if (result.created > 0 || result.duplicates > 0) {
      void globalMutate("/api/transactions");
    }

    const syncedCount = result.created + result.duplicates;
    if (syncedCount > 0) {
      const noun = syncedCount === 1 ? "expense" : "expenses";
      const message =
        remaining > 0
          ? `Synced ${syncedCount} ${noun}, ${remaining} still pending`
          : `Synced ${syncedCount} ${noun}`;
      if (onToast) onToast(message, "success");
      showSyncSystemNotification(message);
    }

    return { created: result.created, duplicates: result.duplicates, remaining };
  } catch (err) {
    for (const item of items) {
      await markAttempt(item, err instanceof Error ? err.message : "Unknown error");
    }
    return { created: 0, duplicates: 0, remaining: await countPending() };
  } finally {
    syncInProgress = false;
    emitSyncState(false);
  }
}
