"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { DashboardSidebar, MobileNav } from "@/components/dashboard/sidebar";
import { SyncProvider } from "@/components/sync-provider";
import { PendingSyncStatus } from "@/components/pending-sync-status";
import { IosInstallBanner } from "@/components/pwa/ios-install-banner";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted/30">
        <p role="status" aria-live="polite" className="text-sm text-muted-foreground">Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="flex min-h-screen bg-muted/30 overflow-x-hidden">
      <SyncProvider />
      <DashboardSidebar />
      <main className="flex-1 min-w-0 p-4 sm:p-6 pb-28 md:pb-6 overflow-y-auto overflow-x-hidden">
        <IosInstallBanner />
        <PendingSyncStatus />
        {children}
      </main>
      <MobileNav />
    </div>
  );
}
