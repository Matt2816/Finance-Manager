"use client";

import { useMemo } from "react";
import useSWR from "swr";
import { MapPin } from "lucide-react";

import { Transaction } from "@/types/transaction";
import {
  aggregateSpendByAddress,
  LocationSpendPoint,
} from "@/lib/transaction-analytics";
import { formatLocationSummary } from "@/lib/location";
import type { GeocodeResult } from "@/pages/api/geocode";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface SpendingLocationHeatmapProps {
  transactions: Transaction[];
}

type BatchGeocodeResponse = {
  results: Record<string, GeocodeResult | null>;
};

async function batchGeocodeFetcher(
  addresses: string[]
): Promise<BatchGeocodeResponse> {
  const res = await fetch("/api/geocode-batch", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ addresses }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(
      typeof data === "object" && data !== null && "error" in data
        ? String((data as { error: string }).error)
        : res.statusText
    );
  }
  return data as BatchGeocodeResponse;
}

function projectPoints(
  points: LocationSpendPoint[],
  width: number,
  height: number,
  padding: number
): Array<LocationSpendPoint & { x: number; y: number; radius: number }> {
  if (points.length === 0) return [];

  const lats = points.map((p) => p.lat);
  const lons = points.map((p) => p.lon);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLon = Math.min(...lons);
  const maxLon = Math.max(...lons);

  const latSpan = maxLat - minLat || 0.01;
  const lonSpan = maxLon - minLon || 0.01;
  const maxSpend = Math.max(...points.map((p) => p.totalSpend));

  return points.map((point) => {
    const x =
      padding +
      ((point.lon - minLon) / lonSpan) * (width - padding * 2);
    const y =
      height -
      padding -
      ((point.lat - minLat) / latSpan) * (height - padding * 2);
    const radius = 6 + (point.totalSpend / maxSpend) * 22;
    return { ...point, x, y, radius };
  });
}

export function SpendingLocationHeatmap({
  transactions,
}: SpendingLocationHeatmapProps) {
  const addressSpend = useMemo(
    () => aggregateSpendByAddress(transactions),
    [transactions]
  );

  const addresses = useMemo(
    () =>
      [...addressSpend.entries()]
        .sort((a, b) => b[1].totalSpend - a[1].totalSpend)
        .slice(0, 25)
        .map(([address]) => address),
    [addressSpend]
  );

  const { data, error, isLoading } = useSWR(
    addresses.length > 0 ? ["geocode-batch", ...addresses] : null,
    () => batchGeocodeFetcher(addresses),
    { revalidateOnFocus: false }
  );

  const points = useMemo(() => {
    if (!data?.results) return [];
    const result: LocationSpendPoint[] = [];
    for (const address of addresses) {
      const geo = data.results[address];
      const stats = addressSpend.get(address);
      if (!geo || !stats) continue;
      result.push({
        address,
        label: formatLocationSummary(address, 40),
        lat: geo.lat,
        lon: geo.lon,
        totalSpend: stats.totalSpend,
        transactionCount: stats.transactionCount,
      });
    }
    return result;
  }, [data, addresses, addressSpend]);

  const projected = useMemo(
    () => projectPoints(points, 360, 220, 24),
    [points]
  );

  if (addresses.length === 0) {
    return (
      <Card className="col-span-full" data-screenshot="heatmap">
        <CardHeader>
          <CardTitle className="text-lg sm:text-xl">Spending heatmap</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            No location data in transactions yet. Import Wallet notes with
            addresses to see a geographic heatmap.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="col-span-full" data-screenshot="heatmap">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg sm:text-xl">
          <MapPin className="h-5 w-5" />
          Spending heatmap
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          Bubble size reflects total spend at each location (
          {projected.length} of {addresses.length} plotted)
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="relative overflow-hidden rounded-lg border bg-muted/20">
          {isLoading && (
            <div className="flex h-[220px] items-center justify-center text-sm text-muted-foreground sm:h-[280px]">
              Plotting locations…
            </div>
          )}
          {!isLoading && error && (
            <div className="flex h-[220px] items-center justify-center px-4 text-center text-sm text-muted-foreground">
              Could not load map data. Try again later.
            </div>
          )}
          {!isLoading && !error && projected.length > 0 && (
            <svg
              viewBox="0 0 360 220"
              className="h-[220px] w-full sm:h-[280px]"
              role="img"
              aria-label="Spending heatmap by location"
            >
              <defs>
                <radialGradient id="heatGradient">
                  <stop offset="0%" stopColor="hsl(var(--chart-1))" stopOpacity="0.85" />
                  <stop offset="100%" stopColor="hsl(var(--chart-1))" stopOpacity="0.15" />
                </radialGradient>
              </defs>
              <rect width="360" height="220" fill="hsl(var(--muted))" opacity="0.35" />
              {projected.map((point) => (
                <g key={point.address}>
                  <circle
                    cx={point.x}
                    cy={point.y}
                    r={point.radius}
                    fill="url(#heatGradient)"
                    stroke="hsl(var(--chart-1))"
                    strokeWidth="1"
                    opacity={0.75}
                  />
                  <circle
                    cx={point.x}
                    cy={point.y}
                    r={3}
                    fill="hsl(var(--chart-1))"
                  />
                  <title>
                    {point.label}: $
                    {point.totalSpend.toFixed(2)} ({point.transactionCount}{" "}
                    transactions)
                  </title>
                </g>
              ))}
            </svg>
          )}
          {!isLoading && !error && projected.length === 0 && (
            <div className="flex h-[220px] items-center justify-center text-sm text-muted-foreground">
              No locations could be geocoded for the heatmap.
            </div>
          )}
        </div>

        {projected.length > 0 && (
          <ul className="grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
            {projected
              .sort((a, b) => b.totalSpend - a.totalSpend)
              .slice(0, 6)
              .map((point) => (
                <li
                  key={point.address}
                  className="flex items-center justify-between gap-2 rounded-md border px-3 py-2"
                >
                  <span className="truncate">{point.label}</span>
                  <span className="shrink-0 tabular-nums font-medium">
                    ${point.totalSpend.toFixed(0)}
                  </span>
                </li>
              ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
