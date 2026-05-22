"use client";

import { useState } from "react";
import useSWR from "swr";
import { ExternalLink, MapPin } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import type { GeocodeResult } from "@/pages/api/geocode";
import {
  formatLocationSummary,
  mapsSearchUrl,
  openStreetMapEmbedUrl,
} from "@/lib/location";
import { cn } from "@/lib/utils";

async function geocodeFetcher(url: string): Promise<GeocodeResult> {
  const res = await fetch(url);
  const data = await res.json();
  if (!res.ok) {
    const message =
      typeof data === "object" && data !== null && "error" in data
        ? String((data as { error: string }).error)
        : res.statusText;
    throw new Error(message);
  }
  return data as GeocodeResult;
}

interface LocationMapPopoverProps {
  address: string;
  className?: string;
}

export function LocationMapPopover({
  address,
  className,
}: LocationMapPopoverProps) {
  const [open, setOpen] = useState(false);
  const trimmedAddress = address.trim();

  const { data, error, isLoading } = useSWR<GeocodeResult>(
    open && trimmedAddress
      ? `/api/geocode?q=${encodeURIComponent(trimmedAddress)}`
      : null,
    geocodeFetcher,
    { revalidateOnFocus: false }
  );

  if (!trimmedAddress) {
    return <span className="text-muted-foreground text-sm">—</span>;
  }

  const summary = formatLocationSummary(trimmedAddress);
  const mapsUrl = mapsSearchUrl(trimmedAddress);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          className={cn(
            "h-auto max-w-[220px] justify-start gap-1.5 px-2 py-1 font-normal",
            className
          )}
        >
          <MapPin className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
          <span className="truncate text-left">{summary}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="start">
        <div className="space-y-0">
          <div className="border-b px-3 py-2 text-sm whitespace-pre-line">
            {trimmedAddress}
          </div>

          <div className="relative h-48 w-full bg-muted">
            {isLoading && (
              <div className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">
                Loading map…
              </div>
            )}
            {!isLoading && data && (
              <iframe
                title={`Map for ${summary}`}
                src={openStreetMapEmbedUrl(data.lat, data.lon)}
                className="h-full w-full border-0"
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            )}
            {!isLoading && !data && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 px-4 text-center text-sm text-muted-foreground">
                <span>
                  {error?.message === "Location not found"
                    ? "Could not plot this address on the map."
                    : "Map preview unavailable."}
                </span>
                <a
                  href={mapsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-primary underline-offset-4 hover:underline"
                >
                  Search in Google Maps
                  <ExternalLink className="h-3.5 w-3.5" />
                </a>
              </div>
            )}
          </div>

          <div className="flex items-center justify-between gap-2 border-t px-3 py-2">
            {data && (
              <p className="truncate text-xs text-muted-foreground">
                {data.displayName}
              </p>
            )}
            <a
              href={mapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={cn(
                "inline-flex shrink-0 items-center gap-1 text-xs font-medium text-primary underline-offset-4 hover:underline",
                !data && "ml-auto"
              )}
            >
              Open in Maps
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
