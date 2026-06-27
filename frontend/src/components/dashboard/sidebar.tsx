"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuth } from "@/context/auth-context";
import { Button } from "@/components/ui/button";
import { QuickAddSheet } from "@/components/quick-add-sheet";
import { MobileMoreSheet } from "@/components/dashboard/mobile-more-sheet";
import {
  LayoutDashboard,
  Receipt,
  Lightbulb,
  TrendingUp,
  Star,
  Wallet,
  Tags,
  Banknote,
  Settings,
  LogOut,
  MoreHorizontal,
  Plus,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export const primaryNavItems: NavItem[] = [
  { href: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { href: "/dashboard/transactions", label: "Transactions", icon: Receipt },
  { href: "/dashboard/insights", label: "Insights", icon: Lightbulb },
  { href: "/dashboard/forecasts", label: "Forecasts", icon: TrendingUp },
];

export const secondaryNavItems: NavItem[] = [
  { href: "/dashboard/income", label: "Income", icon: Banknote },
  { href: "/dashboard/loyalty", label: "Merchant Loyalty", icon: Star },
  { href: "/dashboard/categories", label: "Categories", icon: Tags },
  { href: "/dashboard/settings", label: "Settings", icon: Settings },
];

export const navItems: NavItem[] = [...primaryNavItems, ...secondaryNavItems];

function isSecondaryRouteActive(pathname: string | null) {
  if (!pathname) return false;
  return secondaryNavItems.some((item) => item.href === pathname);
}

export function DashboardSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();

  function handleLogout() {
    logout();
    router.replace("/login");
  }

  return (
    <aside className="hidden md:flex flex-col w-64 border-r bg-background h-screen sticky top-0">
      <div className="p-6">
        <Link href="/dashboard" className="flex items-center gap-2 font-bold text-xl">
          <Wallet className="h-6 w-6 text-primary" />
          <span>Finance Manager</span>
        </Link>
      </div>

      <nav className="flex-1 px-4 space-y-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary/10 text-primary"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t space-y-3">
        {user && (
          <p className="text-xs text-muted-foreground truncate px-1">
            Signed in as <span className="font-medium text-foreground">{user.username}</span>
          </p>
        )}
        <Button variant="outline" size="sm" className="w-full" onClick={handleLogout}>
          <LogOut className="h-4 w-4 mr-2" />
          Log out
        </Button>
        <p className="text-xs text-muted-foreground text-center">
          Finance Manager v0.1.0
        </p>
      </div>
    </aside>
  );
}

export function MobileNav() {
  const pathname = usePathname();
  const [moreOpen, setMoreOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);

  const moreIsActive = isSecondaryRouteActive(pathname);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const params = new URLSearchParams(window.location.search);
    if (params.get("quickAdd") === "1") {
      setAddOpen(true);
      window.history.replaceState(null, "", window.location.pathname);
    }
  }, []);

  return (
    <>
      <nav
        className="md:hidden fixed bottom-0 left-0 right-0 bg-background border-t z-50 pb-[env(safe-area-inset-bottom)]"
        aria-label="Main navigation"
      >
        <div className="relative">
          <button
            type="button"
            onClick={() => setAddOpen(true)}
            aria-label="Add transaction"
            className={cn(
              "absolute -top-5 left-1/2 -translate-x-1/2 z-10",
              "flex h-14 w-14 items-center justify-center rounded-full",
              "bg-primary text-primary-foreground shadow-lg",
              "transition-transform active:scale-95",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            )}
          >
            <Plus className="h-6 w-6" />
          </button>

          <div className="grid grid-cols-5 h-14">
            {primaryNavItems.map((item) => {
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={isActive ? "page" : undefined}
                  className={cn(
                    "flex flex-col items-center justify-center gap-0.5 min-h-11 min-w-11 px-1",
                    "text-[11px] font-medium transition-colors",
                    isActive ? "text-primary" : "text-muted-foreground"
                  )}
                >
                  <item.icon className="h-5 w-5 shrink-0" />
                  <span className="truncate max-w-full">{item.label}</span>
                </Link>
              );
            })}

            <button
              type="button"
              onClick={() => setMoreOpen(true)}
              aria-label="More"
              aria-expanded={moreOpen}
              aria-current={moreIsActive ? "page" : undefined}
              className={cn(
                "flex flex-col items-center justify-center gap-0.5 min-h-11 min-w-11 px-1",
                "text-[11px] font-medium transition-colors",
                moreIsActive ? "text-primary" : "text-muted-foreground"
              )}
            >
              <MoreHorizontal className="h-5 w-5 shrink-0" />
              <span>More</span>
            </button>
          </div>
        </div>
      </nav>

      <MobileMoreSheet
        open={moreOpen}
        onOpenChange={setMoreOpen}
        items={secondaryNavItems}
      />

      <QuickAddSheet open={addOpen} onOpenChange={setAddOpen} />
    </>
  );
}
