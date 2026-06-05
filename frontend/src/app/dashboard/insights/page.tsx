"use client";

import { useState } from "react";
import { PageHeader } from "@/components/dashboard/page-header";
import { useInsights } from "@/hooks/use-analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { AlertTriangle, Info, TrendingUp, Calendar, Filter } from "lucide-react";

export default function InsightsPage() {
  const { insights, isLoading } = useInsights();
  const [filter, setFilter] = useState<string | null>(null);

  const filtered = filter
    ? insights?.filter((i) => i.severity.toUpperCase() === filter)
    : insights;

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

  const severityCounts = {
    HIGH: insights?.filter((i) => i.severity.toUpperCase() === "HIGH").length ?? 0,
    MEDIUM: insights?.filter((i) => i.severity.toUpperCase() === "MEDIUM").length ?? 0,
    LOW: insights?.filter((i) => i.severity.toUpperCase() === "LOW").length ?? 0,
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Insights"
        description="Personalized financial insights and observations."
      />

      <div className="flex flex-wrap gap-2">
        <Button
          variant={filter === null ? "default" : "outline"}
          size="sm"
          onClick={() => setFilter(null)}
        >
          <Filter className="h-3.5 w-3.5 mr-1" />
          All ({insights?.length ?? 0})
        </Button>
        <Button
          variant={filter === "HIGH" ? "default" : "outline"}
          size="sm"
          onClick={() => setFilter("HIGH")}
          className="border-red-200 text-red-700 hover:bg-red-50"
        >
          <AlertTriangle className="h-3.5 w-3.5 mr-1" />
          High ({severityCounts.HIGH})
        </Button>
        <Button
          variant={filter === "MEDIUM" ? "default" : "outline"}
          size="sm"
          onClick={() => setFilter("MEDIUM")}
          className="border-yellow-200 text-yellow-700 hover:bg-yellow-50"
        >
          <AlertTriangle className="h-3.5 w-3.5 mr-1" />
          Medium ({severityCounts.MEDIUM})
        </Button>
        <Button
          variant={filter === "LOW" ? "default" : "outline"}
          size="sm"
          onClick={() => setFilter("LOW")}
          className="border-blue-200 text-blue-700 hover:bg-blue-50"
        >
          <Info className="h-3.5 w-3.5 mr-1" />
          Low ({severityCounts.LOW})
        </Button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-40 bg-muted rounded-lg animate-pulse" />
          ))}
        </div>
      ) : (
        <>
          {filtered && filtered.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filtered.map((insight) => (
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
          ) : (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                {filter ? `No ${filter.toLowerCase()} priority insights.` : "No insights available yet."}
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
