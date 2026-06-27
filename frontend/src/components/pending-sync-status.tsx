"use client";

import { useEffect, useState } from "react";
import { Loader2, CloudUpload } from "lucide-react";
import { countPending } from "@/lib/offline-queue";
import {
  PENDING_CHANGED_EVENT,
  SYNC_STATE_EVENT,
} from "@/lib/sync-pending-transactions";

/**
 * Read-only status of the offline queue. There is no manual sync button by
 * design; the SyncProvider drains the queue automatically.
 */
export function PendingSyncStatus() {
  const [count, setCount] = useState(0);
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const refresh = async () => {
      const c = await countPending();
      if (!cancelled) setCount(c);
    };

    const onSyncState = (e: Event) => {
      const detail = (e as CustomEvent<{ active: boolean }>).detail;
      setSyncing(Boolean(detail?.active));
      void refresh();
    };

    void refresh();
    window.addEventListener(PENDING_CHANGED_EVENT, refresh);
    window.addEventListener(SYNC_STATE_EVENT, onSyncState);

    return () => {
      cancelled = true;
      window.removeEventListener(PENDING_CHANGED_EVENT, refresh);
      window.removeEventListener(SYNC_STATE_EVENT, onSyncState);
    };
  }, []);

  if (count === 0) return null;

  const noun = count === 1 ? "expense" : "expenses";

  return (
    <div
      role="status"
      aria-live="polite"
      className="mb-4 flex items-center gap-2 rounded-lg border bg-muted/50 px-3 py-2 text-sm text-muted-foreground"
    >
      {syncing ? (
        <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
      ) : (
        <CloudUpload className="h-4 w-4 shrink-0" />
      )}
      <span>
        {count} {noun} waiting to sync
        {syncing ? "…" : " — will sync automatically"}
      </span>
    </div>
  );
}
