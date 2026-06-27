export function isIOS(): boolean {
  if (typeof navigator === "undefined") return false;
  return /iPhone|iPad|iPod/.test(navigator.userAgent);
}

export function isStandalone(): boolean {
  if (typeof window === "undefined") return false;
  const nav = navigator as Navigator & { standalone?: boolean };
  return (
    nav.standalone === true ||
    window.matchMedia("(display-mode: standalone)").matches
  );
}

/**
 * True only for real Safari, not in-app browsers (Mail, Slack, Instagram) which
 * cannot Add to Home Screen.
 */
export function isSafari(): boolean {
  if (typeof navigator === "undefined") return false;
  const ua = navigator.userAgent;
  return /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS|OPiOS|Mercury/.test(ua);
}

export function shouldShowInstallPrompt(): boolean {
  return isIOS() && !isStandalone();
}
