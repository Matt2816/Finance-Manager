"use client";

import { useState } from "react";
import { PageHeader } from "@/components/dashboard/page-header";
import { useMerchantLoyalty } from "@/hooks/use-analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Star, TrendingUp, Calendar, DollarSign, ShoppingBag, Search } from "lucide-react";

export default function LoyaltyPage() {
  const { loyalty, isLoading, error } = useMerchantLoyalty(undefined, 50);
  const [search, setSearch] = useState("");

  const filtered = loyalty?.filter((m) =>
    m.canonicalName.toLowerCase().includes(search.toLowerCase())
  );

  const getLoyaltyColor = (score: number) => {
    if (score >= 80) return "bg-green-100 text-green-800 border-green-200";
    if (score >= 60) return "bg-blue-100 text-blue-800 border-blue-200";
    if (score >= 40) return "bg-yellow-100 text-yellow-800 border-yellow-200";
    return "bg-gray-100 text-gray-800 border-gray-200";
  };

  const getLoyaltyLabel = (score: number) => {
    if (score >= 80) return "Loyal";
    if (score >= 60) return "Regular";
    if (score >= 40) return "Occasional";
    return "New";
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Merchant Loyalty"
        description="Track spending patterns and loyalty scores for your favorite merchants."
      />

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search merchants..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9 max-w-md"
        />
      </div>

      {isLoading && (
        <div className="space-y-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-24 bg-muted rounded-lg animate-pulse" />
          ))}
        </div>
      )}

      {error && (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            Error loading loyalty metrics: {error.message}
          </CardContent>
        </Card>
      )}

      {!isLoading && !error && (
        <div className="space-y-3">
          {filtered?.map((merchant) => (
            <Card
              key={merchant.id}
              className="hover:shadow-sm transition-shadow"
            >
              <CardContent className="p-5">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="font-semibold text-base truncate">
                        {merchant.canonicalName}
                      </h3>
                      <Badge className={getLoyaltyColor(merchant.loyaltyScore)}>
                        {getLoyaltyLabel(merchant.loyaltyScore)}
                      </Badge>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-y-2 gap-x-6 text-sm text-muted-foreground">
                      <div className="flex items-center gap-1.5">
                        <ShoppingBag className="h-3.5 w-3.5" />
                        <span>{merchant.totalTransactions} visits</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <DollarSign className="h-3.5 w-3.5" />
                        <span>${merchant.totalSpend.toFixed(2)} total</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <Calendar className="h-3.5 w-3.5" />
                        <span>Avg ${merchant.avgTransactionSize.toFixed(2)}</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <TrendingUp className="h-3.5 w-3.5" />
                        <span>
                          {merchant.spendGrowthRate !== null
                            ? `${merchant.spendGrowthRate >= 0 ? "+" : ""}${(merchant.spendGrowthRate * 100).toFixed(1)}%`
                            : "N/A"}
                        </span>
                      </div>
                    </div>
                    <p className="text-xs text-muted-foreground mt-2">
                      First visit: {new Date(merchant.firstVisit).toLocaleDateString()} &middot;{" "}
                      Last visit: {new Date(merchant.lastVisit).toLocaleDateString()}
                      {merchant.visitFrequencyDays && (
                        <> &middot; Every {merchant.visitFrequencyDays.toFixed(0)} days</>
                      )}
                    </p>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <div className="text-right">
                      <div className="text-2xl font-bold text-primary">
                        {merchant.loyaltyScore.toFixed(0)}
                      </div>
                      <div className="text-xs text-muted-foreground">Loyalty Score</div>
                    </div>
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <Star className="h-5 w-5 text-primary" />
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}

          {filtered?.length === 0 && (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                {search
                  ? `No merchants matching "${search}".`
                  : "No loyalty data available. Run analytics refresh to generate metrics."}
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
