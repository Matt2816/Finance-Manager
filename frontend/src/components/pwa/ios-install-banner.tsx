"use client";

import { useEffect, useState } from "react";
import { Share, Plus, X } from "lucide-react";
import { isIOS, isSafari, isStandalone } from "@/lib/pwa-utils";

const DISMISS_KEY = "pwa-install-dismissed";

/**
 * iOS has no automatic install prompt, so we guide the user through Safari's
 * Add to Home Screen flow. Shown only on iOS, when not already installed, and
 * not previously dismissed.
 */
export function IosInstallBanner() {
  const [visible, setVisible] = useState(false);
  const [inSafari, setInSafari] = useState(true);

  useEffect(() => {
    if (!isIOS() || isStandalone()) return;
    if (typeof localStorage !== "undefined" && localStorage.getItem(DISMISS_KEY)) return;
    setInSafari(isSafari());
    setVisible(true);
  }, []);

  if (!visible) return null;

  function dismiss() {
    if (typeof localStorage !== "undefined") localStorage.setItem(DISMISS_KEY, "true");
    setVisible(false);
  }

  return (
    <div className="md:hidden mb-4 rounded-xl border bg-card p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <h2 className="text-sm font-semibold">Add Finance Manager to your home screen</h2>
        <button
          type="button"
          onClick={dismiss}
          aria-label="Dismiss"
          className="shrink-0 text-muted-foreground hover:text-foreground min-h-11 min-w-11 -mt-2 -mr-2 flex items-center justify-center"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {inSafari ? (
        <ol className="mt-2 space-y-1.5 text-sm text-muted-foreground">
          <li className="flex items-center gap-2">
            <span>1. Tap</span>
            <Share className="h-4 w-4 inline" aria-label="Share" />
            <span>Share in Safari</span>
          </li>
          <li className="flex items-center gap-2">
            <span>2. Tap</span>
            <Plus className="h-4 w-4 inline" aria-label="Add" />
            <span>Add to Home Screen</span>
          </li>
          <li>3. Tap Add</li>
        </ol>
      ) : (
        <p className="mt-2 text-sm text-muted-foreground">
          Open this page in Safari to install it. This browser cannot add apps to your home screen.
        </p>
      )}

      <button
        type="button"
        onClick={dismiss}
        className="mt-3 text-sm font-medium text-primary"
      >
        Got it
      </button>
    </div>
  );
}
