/** Best-effort city from multi-line Wallet addresses (street / city prov postal / country). */
export function extractCityFromAddress(address: string): string {
  const lines = address
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length === 0) return "Unknown";
  if (lines.length === 1) {
    const parts = lines[0].split(",");
    return parts.length > 1 ? parts[parts.length - 1].trim() : lines[0];
  }
  const cityLine = lines[1];
  const withoutPostal = cityLine.replace(
    /\s+[A-Z]{2}\s+[A-Z0-9]{3}\s?[A-Z0-9]{3}\s*$/i,
    ""
  );
  const withoutProvince = withoutPostal.replace(/\s+[A-Z]{2}\s*$/i, "").trim();
  return withoutProvince || cityLine || "Unknown";
}

export function formatLocationSummary(address: string, maxLength = 32): string {
  const firstLine = address.split("\n").map((line) => line.trim()).find(Boolean);
  if (!firstLine) {
    return "View location";
  }
  if (firstLine.length <= maxLength) {
    return firstLine;
  }
  return `${firstLine.slice(0, maxLength - 1)}…`;
}

export function mapsSearchUrl(address: string): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`;
}

export function openStreetMapEmbedUrl(lat: number, lon: number): string {
  const deltaLon = 0.008;
  const deltaLat = 0.005;
  const bbox = [
    lon - deltaLon,
    lat - deltaLat,
    lon + deltaLon,
    lat + deltaLat,
  ].join(",");
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat}%2C${lon}`;
}
