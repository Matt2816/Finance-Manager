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
