import { Skeleton } from "@/components/ui/skeleton";

export function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Transaction Summary Skeletons */}
        {[...Array(3)].map((_, i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="h-4 w-[200px]" />
            <Skeleton className="h-8 w-[100px]" />
          </div>
        ))}
      </div>

      {/* Transaction Chart Skeleton */}
      <Skeleton className="h-[300px] w-full" />

      {/* Transaction List Skeleton */}
      <div className="space-y-2">
        <Skeleton className="h-4 w-[200px]" />
        <div className="space-y-1">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      </div>
    </div>
  );
}
