"use client";

import { useEffect, useRef } from "react";
import { useToast } from "@/components/toast-provider";
import {
  syncPendingTransactions,
  PENDING_CHANGED_EVENT,
} from "@/lib/sync-pending-transactions";
import { countPending } from "@/lib/offline-queue";

const DEBOUNCE_MS = 2000;
const POLL_MS = 30000;

/**
 * Owns all automatic offline-queue sync triggers. There is no manual sync action;
 * the queue drains on reconnect, app foreground, focus, and a periodic poll while
 * items remain. Mounted once inside the dashboard layout.
 */
export function SyncProvider() {
  const { showToast } = useToast();
  const showToastRef = useRef(showToast);
  showToastRef.current = showToast;

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let cancelled = false;

    const runSync = () => {
      void syncPendingTransactions((message, type) => showToastRef.current(message, type));
    };

    const debouncedSync = () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        if (!cancelled) runSync();
      }, DEBOUNCE_MS);
    };

    const onVisibility = () => {
      if (document.visibilityState === "visible") debouncedSync();
    };

    pollRef.current = setInterval(async () => {
      const count = await countPending();
      if (count > 0 && navigator.onLine) runSync();
    }, POLL_MS);

    window.addEventListener("online", debouncedSync);
    window.addEventListener("focus", debouncedSync);
    window.addEventListener("pageshow", debouncedSync);
    window.addEventListener(PENDING_CHANGED_EVENT, debouncedSync);
    document.addEventListener("visibilitychange", onVisibility);

    debouncedSync();

    return () => {
      cancelled = true;
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (pollRef.current) clearInterval(pollRef.current);
      window.removeEventListener("online", debouncedSync);
      window.removeEventListener("focus", debouncedSync);
      window.removeEventListener("pageshow", debouncedSync);
      window.removeEventListener(PENDING_CHANGED_EVENT, debouncedSync);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return null;
}
