import { Suspense } from "react";
import { TransactionDashboard } from "@/components/transaction-dashboard";
import { DashboardSkeleton } from "@/components/dashboard-skeleton";
export default function Home() {
  return (
    <main className="container mx-auto p-4">
      <h1 className="text-3xl font-bold mb-6">Financial Dashboard</h1>
      <Suspense fallback={<DashboardSkeleton />}>
        <TransactionDashboard />
      </Suspense>
    </main>
  );
}
