import { NextApiRequest, NextApiResponse } from "next";

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    console.log("fetching transactions");
    console.log(process.env.NEXT_PUBLIC_API_URL);
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/transaction`
    );
    const data = await response.json();
    res.status(200).json(data);
  } catch (error) {
    res.status(500).json({ error: "Error fetching transactions" });
  }
}
