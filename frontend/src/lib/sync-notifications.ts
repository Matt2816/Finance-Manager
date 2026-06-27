const ASKED_KEY = "notifications-asked";

function notificationsSupported(): boolean {
  return typeof window !== "undefined" && "Notification" in window;
}

/**
 * Ask for notification permission at most once. We never re-prompt if the user
 * already answered (granted or denied) to avoid nagging. iOS only grants this
 * for PWAs installed to the home screen (iOS 16.4+).
 */
export async function ensureNotificationPermission(): Promise<void> {
  if (!notificationsSupported()) return;
  if (Notification.permission !== "default") return;
  if (typeof localStorage !== "undefined" && localStorage.getItem(ASKED_KEY)) return;

  try {
    if (typeof localStorage !== "undefined") localStorage.setItem(ASKED_KEY, "true");
    await Notification.requestPermission();
  } catch {
    // Safari can throw if called outside a user gesture; ignore.
  }
}

/**
 * Show a system notification only when the app is backgrounded. When the app is
 * visible we rely on the in-app toast instead, to avoid duplicate alerts.
 */
export function showSyncSystemNotification(message: string): void {
  if (!notificationsSupported()) return;
  if (Notification.permission !== "granted") return;
  if (typeof document !== "undefined" && document.visibilityState === "visible") return;

  try {
    new Notification("Finance Manager", { body: message, icon: "/icons/icon-192.png" });
  } catch {
    // Some browsers require notifications via a service worker; ignore failures.
  }
}

export function setPendingBadge(count: number): void {
  if (typeof navigator === "undefined") return;
  const nav = navigator as Navigator & {
    setAppBadge?: (n?: number) => Promise<void>;
    clearAppBadge?: () => Promise<void>;
  };
  try {
    if (count > 0 && nav.setAppBadge) {
      void nav.setAppBadge(count);
    } else if (nav.clearAppBadge) {
      void nav.clearAppBadge();
    }
  } catch {
    // Badge API not available; ignore.
  }
}
