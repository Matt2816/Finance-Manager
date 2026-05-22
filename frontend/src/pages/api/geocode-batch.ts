import type { NextApiRequest, NextApiResponse } from "next";

import type { GeocodeResult } from "@/pages/api/geocode";

const MAX_ADDRESSES = 25;
const DELAY_MS = 200;

type BatchResult = {
  results: Record<string, GeocodeResult | null>;
};

async function geocodeOne(query: string): Promise<GeocodeResult | null> {
  const url = new URL("https://nominatim.openstreetmap.org/search");
  url.searchParams.set("format", "json");
  url.searchParams.set("limit", "1");
  url.searchParams.set("q", query);

  const response = await fetch(url, {
    headers: {
      "User-Agent": "Finance-Manager/1.0 (local dev)",
      Accept: "application/json",
    },
  });

  if (!response.ok) return null;

  const results = (await response.json()) as Array<{
    lat: string;
    lon: string;
    display_name: string;
  }>;

  if (!results.length) return null;

  const [match] = results;
  return {
    lat: Number.parseFloat(match.lat),
    lon: Number.parseFloat(match.lon),
    displayName: match.display_name,
  };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse<BatchResult | { error: string }>
) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "Method not allowed" });
  }

  const body = req.body as { addresses?: unknown };
  if (!body || !Array.isArray(body.addresses)) {
    return res.status(400).json({ error: "Expected { addresses: string[] }" });
  }

  const unique = [
    ...new Set(
      body.addresses
        .filter((a): a is string => typeof a === "string")
        .map((a) => a.trim())
        .filter(Boolean)
    ),
  ].slice(0, MAX_ADDRESSES);

  const results: Record<string, GeocodeResult | null> = {};

  for (let i = 0; i < unique.length; i++) {
    if (i > 0) await sleep(DELAY_MS);
    const address = unique[i];
    try {
      results[address] = await geocodeOne(address);
    } catch {
      results[address] = null;
    }
  }

  return res.status(200).json({ results });
}
