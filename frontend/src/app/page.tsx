import { Suspense } from "react";
import { TransactionDashboard } from "@/components/transaction-dashboard";
import { DashboardSkeleton } from "@/components/dashboard-skeleton";
export default function Home() {
  return (
    <main className="container mx-auto max-w-7xl px-3 py-4 sm:px-4 sm:py-6">
      <h1 className="mb-4 text-2xl font-bold tracking-tight sm:mb-6 sm:text-3xl">
        Financial Dashboard
      </h1>
      <Suspense fallback={<DashboardSkeleton />}>
        <TransactionDashboard />
      </Suspense>
    </main>
  );
}
