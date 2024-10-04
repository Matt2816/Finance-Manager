import { z } from "zod";

import { transactionSchema } from "@/components/data/schema";

export type Transaction = z.infer<typeof transactionSchema>;
