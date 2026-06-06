/**
 * Format a YYYY-MM-DD date string for display in EDT.
 */
export function formatDateEDT(dateStr: string): string {
  if (!dateStr) return "";
  const d = new Date(dateStr + "T00:00:00");
  return d.toLocaleDateString("en-US", { timeZone: "America/New_York" });
}

/**
 * Format an ISO timestamp for display in EDT.
 */
export function formatTimestampEDT(isoString: string): string {
  if (!isoString) return "";
  const d = new Date(isoString);
  return d.toLocaleDateString("en-US", {
    timeZone: "America/New_York",
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
