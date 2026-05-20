import type { NextApiRequest, NextApiResponse } from "next";
import { fetchBackend } from "@/lib/backend-api";
import type { Transaction } from "@/types/transaction";

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse<Transaction[] | { error: string }>
) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ error: "Method not allowed" });
  }

  try {
    const data = await fetchBackend<Transaction[]>("/transaction");
    return res.status(200).json(Array.isArray(data) ? data : []);
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Error fetching transactions";
    console.error("GET /api/transactions:", message);
    return res.status(500).json({ error: message });
  }
}
