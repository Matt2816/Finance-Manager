"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useInsights } from "@/hooks/use-analytics";
import { Badge } from "@/components/ui/badge";
import { AlertTriangle, Info, TrendingUp, Calendar } from "lucide-react";

export function InsightCardsGrid() {
  const { insights, isLoading } = useInsights();

  if (isLoading) {
    return <div>Loading insights...</div>;
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity.toUpperCase()) {
      case "HIGH":
        return <AlertTriangle className="h-4 w-4 text-red-500" />;
      case "MEDIUM":
        return <AlertTriangle className="h-4 w-4 text-yellow-500" />;
      default:
        return <Info className="h-4 w-4 text-blue-500" />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity.toUpperCase()) {
      case "HIGH":
        return "bg-red-100 text-red-800 border-red-200";
      case "MEDIUM":
        return "bg-yellow-100 text-yellow-800 border-yellow-200";
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  const getInsightIcon = (type: string) => {
    if (type.includes("TREND")) return <TrendingUp className="h-4 w-4" />;
    if (type.includes("FREQUENCY")) return <Calendar className="h-4 w-4" />;
    return <Info className="h-4 w-4" />;
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {insights?.map((insight) => (
        <Card key={insight.id} className="hover:shadow-md transition-shadow">
          <CardHeader className="pb-3">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2">
                {getInsightIcon(insight.insightType)}
                <CardTitle className="text-sm font-medium">
                  {insight.title}
                </CardTitle>
              </div>
              <Badge className={getSeverityColor(insight.severity)}>
                {insight.severity}
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-3">
              {insight.body}
            </p>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              {getSeverityIcon(insight.severity)}
              <span>Valid until {new Date(insight.validTo).toLocaleDateString()}</span>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
