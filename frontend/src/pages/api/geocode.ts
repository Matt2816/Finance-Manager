import type { NextApiRequest, NextApiResponse } from "next";

export type GeocodeResult = {
  lat: number;
  lon: number;
  displayName: string;
};

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse<GeocodeResult | { error: string }>
) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ error: "Method not allowed" });
  }

  const query = typeof req.query.q === "string" ? req.query.q.trim() : "";
  if (!query) {
    return res.status(400).json({ error: "Missing query parameter q" });
  }

  try {
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

    if (!response.ok) {
      return res.status(502).json({ error: "Geocoding service unavailable" });
    }

    const results = (await response.json()) as Array<{
      lat: string;
      lon: string;
      display_name: string;
    }>;

    if (!results.length) {
      return res.status(404).json({ error: "Location not found" });
    }

    const [match] = results;
    return res.status(200).json({
      lat: Number.parseFloat(match.lat),
      lon: Number.parseFloat(match.lon),
      displayName: match.display_name,
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Geocoding request failed";
    console.error("GET /api/geocode:", message);
    return res.status(500).json({ error: message });
  }
}
