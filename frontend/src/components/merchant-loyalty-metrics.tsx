"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useMerchantLoyalty } from "@/hooks/use-analytics";
import { Badge } from "@/components/ui/badge";
import { Star, TrendingUp, Calendar, DollarSign, ShoppingBag } from "lucide-react";

export function MerchantLoyaltyMetrics() {
  const { loyalty, isLoading, error } = useMerchantLoyalty(undefined, 10);

  if (isLoading) {
    return <div>Loading loyalty metrics...</div>;
  }

  if (error) {
    return <div>Error loading loyalty metrics: {error.message}</div>;
  }

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
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Star className="h-5 w-5" />
          Merchant Loyalty Metrics
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {loyalty?.map((merchant) => (
            <div
              key={merchant.id}
              className="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h4 className="font-semibold">{merchant.canonicalName}</h4>
                  <Badge className={getLoyaltyColor(merchant.loyaltyScore)}>
                    {getLoyaltyLabel(merchant.loyaltyScore)}
                  </Badge>
                </div>
                <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
                  <div className="flex items-center gap-1">
                    <ShoppingBag className="h-3 w-3" />
                    <span>{merchant.totalTransactions} visits</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <DollarSign className="h-3 w-3" />
                    <span>${merchant.totalSpend.toFixed(2)} total</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    <span>Avg ${merchant.avgTransactionSize.toFixed(2)}</span>
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold text-primary">
                  {merchant.loyaltyScore.toFixed(0)}
                </div>
                <div className="text-xs text-muted-foreground">Loyalty Score</div>
                {merchant.spendGrowthRate !== null && (
                  <div className="flex items-center gap-1 text-xs mt-1">
                    <TrendingUp className="h-3 w-3" />
                    <span className={merchant.spendGrowthRate >= 0 ? "text-green-600" : "text-red-600"}>
                      {merchant.spendGrowthRate >= 0 ? "+" : ""}
                      {(merchant.spendGrowthRate * 100).toFixed(1)}% growth
                    </span>
                  </div>
                )}
              </div>
            </div>
          ))}
          {loyalty?.length === 0 && (
            <div className="text-center py-8 text-muted-foreground">
              No loyalty data available. Run analytics refresh to generate metrics.
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
