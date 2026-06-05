"use client";

import { useState, useCallback, useEffect } from "react";
import { PageHeader } from "@/components/dashboard/page-header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/toast-provider";
import { getApiBaseUrl } from "@/lib/api-config";
import { authenticatedFetch, authenticatedJson } from "@/lib/authenticated-fetch";
import {
  RefreshCw,
  Activity,
  HeartPulse,
  TrendingUp,
  Lightbulb,
  Loader2,
  CheckCircle,
  AlertCircle,
  DollarSign,
  Wallet,
  PiggyBank,
  Percent,
  Users,
} from "lucide-react";

interface HealthResponse {
  status: string;
  message: string;
  timestamp: string;
  clientAddress: string;
}

interface AnalyticsStatus {
  lastRefreshAt: string | null;
  status: string;
  recordCount: number;
}

interface IncomeSummary {
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
}

interface InsightItem {
  id: string;
  title: string;
  insightType: string;
}

interface ForecastItem {
  id: string;
  forecastDate: string;
  projectedAmount: number;
}

interface SplitwiseConfig {
  configured: boolean;
  keyHint: string | null;
  groupNames: string | null;
  enabled: boolean;
}

interface SplitwiseStatus {
  lastStatus: string;
  lastStartedAt: string | null;
  lastFinishedAt: string | null;
  lastPollAt: string | null;
  lastSuccessAt: string | null;
  lastError: string | null;
  importedCount: number;
  updatedCount: number;
  deletedCount: number;
  skippedCount: number;
}

export default function SettingsPage() {
  const { showToast } = useToast();
  const baseUrl = getApiBaseUrl();

  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [healthLoading, setHealthLoading] = useState(false);

  const [analyticsStatus, setAnalyticsStatus] = useState<AnalyticsStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);

  const [incomeSummary, setIncomeSummary] = useState<IncomeSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  const [insights, setInsights] = useState<InsightItem[] | null>(null);
  const [insightsLoading, setInsightsLoading] = useState(false);

  const [forecasts, setForecasts] = useState<ForecastItem[] | null>(null);
  const [forecastsLoading, setForecastsLoading] = useState(false);

  const [refreshing, setRefreshing] = useState(false);

  const [splitwiseConfig, setSplitwiseConfig] = useState<SplitwiseConfig | null>(null);
  const [splitwiseStatus, setSplitwiseStatus] = useState<SplitwiseStatus | null>(null);
  const [splitwiseApiKey, setSplitwiseApiKey] = useState("");
  const [splitwiseGroupNames, setSplitwiseGroupNames] = useState("");
  const [splitwiseEnabled, setSplitwiseEnabled] = useState(false);
  const [splitwiseSaving, setSplitwiseSaving] = useState(false);
  const [splitwiseSyncing, setSplitwiseSyncing] = useState(false);

  const fetchHealth = useCallback(async () => {
    setHealthLoading(true);
    try {
      const data = await authenticatedJson<HealthResponse>(`${baseUrl}/api/health`);
      setHealth(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Health check failed", "error");
    } finally {
      setHealthLoading(false);
    }
  }, [baseUrl, showToast]);

  const fetchAnalyticsStatus = useCallback(async () => {
    setStatusLoading(true);
    try {
      const data = await authenticatedJson<AnalyticsStatus>(`${baseUrl}/api/analytics/status`);
      setAnalyticsStatus(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to fetch analytics status", "error");
    } finally {
      setStatusLoading(false);
    }
  }, [baseUrl, showToast]);

  const fetchIncomeSummary = useCallback(async () => {
    setSummaryLoading(true);
    try {
      const data = await authenticatedJson<IncomeSummary>(`${baseUrl}/api/analytics/income-summary`);
      setIncomeSummary(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to fetch income summary", "error");
    } finally {
      setSummaryLoading(false);
    }
  }, [baseUrl, showToast]);

  const fetchInsights = useCallback(async () => {
    setInsightsLoading(true);
    try {
      const data = await authenticatedJson<InsightItem[]>(`${baseUrl}/api/insights`);
      setInsights(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to fetch insights", "error");
    } finally {
      setInsightsLoading(false);
    }
  }, [baseUrl, showToast]);

  const fetchForecasts = useCallback(async () => {
    setForecastsLoading(true);
    try {
      const data = await authenticatedJson<ForecastItem[]>(`${baseUrl}/api/forecasts`);
      setForecasts(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to fetch forecasts", "error");
    } finally {
      setForecastsLoading(false);
    }
  }, [baseUrl, showToast]);

  const fetchSplitwiseConfig = useCallback(async () => {
    try {
      const data = await authenticatedJson<SplitwiseConfig>(`${baseUrl}/api/splitwise/config`);
      setSplitwiseConfig(data);
      setSplitwiseGroupNames(data.groupNames ?? "");
      setSplitwiseEnabled(data.enabled);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to load Splitwise config", "error");
    }
  }, [baseUrl, showToast]);

  const fetchSplitwiseStatus = useCallback(async () => {
    try {
      const data = await authenticatedJson<SplitwiseStatus>(`${baseUrl}/api/splitwise/status`);
      setSplitwiseStatus(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to load Splitwise status", "error");
    }
  }, [baseUrl, showToast]);

  const saveSplitwiseConfig = useCallback(async () => {
    setSplitwiseSaving(true);
    try {
      const body: { apiKey?: string; groupNames: string; enabled: boolean } = {
        groupNames: splitwiseGroupNames,
        enabled: splitwiseEnabled,
      };
      if (splitwiseApiKey.trim()) {
        body.apiKey = splitwiseApiKey.trim();
      }
      const data = await authenticatedJson<SplitwiseConfig>(`${baseUrl}/api/splitwise/config`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      setSplitwiseConfig(data);
      setSplitwiseApiKey("");
      showToast("Splitwise settings saved");
      fetchSplitwiseStatus();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to save Splitwise config", "error");
    } finally {
      setSplitwiseSaving(false);
    }
  }, [
    baseUrl,
    showToast,
    splitwiseApiKey,
    splitwiseGroupNames,
    splitwiseEnabled,
    fetchSplitwiseStatus,
  ]);

  const triggerSplitwiseSync = useCallback(async () => {
    setSplitwiseSyncing(true);
    try {
      const res = await authenticatedFetch(`${baseUrl}/api/splitwise/sync`, { method: "POST" });
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      showToast(data.message || "Splitwise sync started");
      setTimeout(() => fetchSplitwiseStatus(), 2000);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Splitwise sync failed", "error");
    } finally {
      setSplitwiseSyncing(false);
    }
  }, [baseUrl, showToast, fetchSplitwiseStatus]);

  const triggerRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      const res = await authenticatedFetch(`${baseUrl}/api/analytics/refresh`, { method: "POST" });
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      showToast(data.message || "Analytics refresh started");
      // Refresh status after a short delay
      setTimeout(() => fetchAnalyticsStatus(), 2000);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Refresh failed", "error");
    } finally {
      setRefreshing(false);
    }
  }, [baseUrl, showToast, fetchAnalyticsStatus]);

  // Auto-load stats on mount
  useEffect(() => {
    fetchHealth();
    fetchAnalyticsStatus();
    fetchIncomeSummary();
    fetchInsights();
    fetchForecasts();
    fetchSplitwiseConfig();
    fetchSplitwiseStatus();
  }, [
    fetchHealth,
    fetchAnalyticsStatus,
    fetchIncomeSummary,
    fetchInsights,
    fetchForecasts,
    fetchSplitwiseConfig,
    fetchSplitwiseStatus,
  ]);

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(val);

  const formatPercent = (val: number) =>
    new Intl.NumberFormat("en-US", { style: "percent", minimumFractionDigits: 1 }).format(val / 100);

  return (
    <div className="space-y-6 max-w-5xl">
      <PageHeader title="Settings &amp; Admin" description="System status, analytics controls, and quick stats." />

      {/* Actions */}
      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Actions</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <Button
            onClick={triggerRefresh}
            disabled={refreshing}
            className="h-12 justify-start gap-3 text-base"
          >
            {refreshing ? <Loader2 className="h-5 w-5 animate-spin" /> : <RefreshCw className="h-5 w-5" />}
            Refresh Analytics
          </Button>
          <Button
            onClick={fetchHealth}
            disabled={healthLoading}
            variant="outline"
            className="h-12 justify-start gap-3 text-base"
          >
            {healthLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : <HeartPulse className="h-5 w-5" />}
            Check Health
          </Button>
          <Button
            onClick={fetchAnalyticsStatus}
            disabled={statusLoading}
            variant="outline"
            className="h-12 justify-start gap-3 text-base"
          >
            {statusLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : <Activity className="h-5 w-5" />}
            Refresh Status
          </Button>
        </div>
      </section>

      {/* System Status */}
      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">System Status</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* Health */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <HeartPulse className="h-4 w-4 text-primary" />
                API Health
              </CardTitle>
            </CardHeader>
            <CardContent>
              {health ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    {health.status === "ok" ? (
                      <CheckCircle className="h-5 w-5 text-green-500" />
                    ) : (
                      <AlertCircle className="h-5 w-5 text-red-500" />
                    )}
                    <span className="font-medium capitalize">{health.status}</span>
                  </div>
                  <p className="text-sm text-muted-foreground">{health.message}</p>
                  <p className="text-xs text-muted-foreground">{health.timestamp}</p>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>

          {/* Analytics Status */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Activity className="h-4 w-4 text-primary" />
                Analytics Status
              </CardTitle>
            </CardHeader>
            <CardContent>
              {analyticsStatus ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <CheckCircle className="h-5 w-5 text-green-500" />
                    <span className="font-medium capitalize">{analyticsStatus.status}</span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Records: <span className="font-medium text-foreground">{analyticsStatus.recordCount}</span>
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Last refresh: {analyticsStatus.lastRefreshAt ?? "Never"}
                  </p>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>

          {/* Insights Count */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Lightbulb className="h-4 w-4 text-primary" />
                Insights
              </CardTitle>
            </CardHeader>
            <CardContent>
              {insights ? (
                <div className="space-y-2">
                  <p className="text-2xl font-bold">{insights.length}</p>
                  <p className="text-sm text-muted-foreground">Active insights available</p>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Income Summary */}
      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Month-to-Date Summary
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <DollarSign className="h-4 w-4 text-green-500" />
                Total Income
              </CardTitle>
            </CardHeader>
            <CardContent>
              {incomeSummary ? (
                <p className="text-2xl font-bold">{formatCurrency(incomeSummary.totalIncome)}</p>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Wallet className="h-4 w-4 text-red-500" />
                Total Expenses
              </CardTitle>
            </CardHeader>
            <CardContent>
              {incomeSummary ? (
                <p className="text-2xl font-bold">{formatCurrency(incomeSummary.totalExpenses)}</p>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <PiggyBank className="h-4 w-4 text-blue-500" />
                Net Savings
              </CardTitle>
            </CardHeader>
            <CardContent>
              {incomeSummary ? (
                <p className="text-2xl font-bold">{formatCurrency(incomeSummary.netSavings)}</p>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Percent className="h-4 w-4 text-purple-500" />
                Savings Rate
              </CardTitle>
            </CardHeader>
            <CardContent>
              {incomeSummary ? (
                <p className="text-2xl font-bold">{formatPercent(incomeSummary.savingsRate)}</p>
              ) : (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Splitwise Integration */}
      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Splitwise Integration
        </h2>
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Users className="h-4 w-4 text-primary" />
              Splitwise
            </CardTitle>
            <CardDescription>
              Import debts you owe from Splitwise. API key is encrypted at rest and never shown after save.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2">
              <Label htmlFor="splitwise-api-key">API Key</Label>
              <Input
                id="splitwise-api-key"
                type="password"
                placeholder={
                  splitwiseConfig?.configured
                    ? `Configured ${splitwiseConfig.keyHint ?? ""} — enter new key to replace`
                    : "Paste your Splitwise API key"
                }
                value={splitwiseApiKey}
                onChange={(e) => setSplitwiseApiKey(e.target.value)}
                autoComplete="off"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="splitwise-groups">Group names (comma-separated)</Label>
              <Input
                id="splitwise-groups"
                placeholder="Leave blank to sync all groups"
                value={splitwiseGroupNames}
                onChange={(e) => setSplitwiseGroupNames(e.target.value)}
              />
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={splitwiseEnabled}
                onChange={(e) => setSplitwiseEnabled(e.target.checked)}
                className="rounded border-input"
              />
              Enable automatic daily sync
            </label>
            <div className="flex flex-wrap gap-2">
              <Button onClick={saveSplitwiseConfig} disabled={splitwiseSaving}>
                {splitwiseSaving ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                Test &amp; Save
              </Button>
              <Button
                variant="outline"
                onClick={triggerSplitwiseSync}
                disabled={splitwiseSyncing || !splitwiseConfig?.configured}
              >
                {splitwiseSyncing ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <RefreshCw className="h-4 w-4 mr-2" />}
                Sync Now
              </Button>
            </div>
            {splitwiseStatus && (
              <div className="rounded-lg border px-3 py-2 text-sm space-y-1">
                <p>
                  Last run: <span className="font-medium">{splitwiseStatus.lastStatus}</span>
                  {splitwiseStatus.lastFinishedAt ? ` at ${splitwiseStatus.lastFinishedAt}` : ""}
                </p>
                <p className="text-muted-foreground">
                  Imported {splitwiseStatus.importedCount}, updated {splitwiseStatus.updatedCount}, deleted{" "}
                  {splitwiseStatus.deletedCount}, skipped {splitwiseStatus.skippedCount}
                </p>
                {splitwiseStatus.lastError && (
                  <p className="text-red-500 text-xs">{splitwiseStatus.lastError}</p>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      {/* Forecasts */}
      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Forecasts</h2>
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <TrendingUp className="h-4 w-4 text-primary" />
              Spending Forecasts
            </CardTitle>
            <CardDescription>
              {forecasts ? `${forecasts.length} forecast entries loaded` : "Loading..."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {forecasts && forecasts.length > 0 ? (
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {forecasts.slice(0, 6).map((f) => (
                  <div
                    key={f.id}
                    className="flex items-center justify-between rounded-lg border px-3 py-2 text-sm"
                  >
                    <span className="text-muted-foreground">{f.forecastDate}</span>
                    <span className="font-medium">{formatCurrency(f.projectedAmount)}</span>
                  </div>
                ))}
                {forecasts.length > 6 && (
                  <p className="text-xs text-muted-foreground text-center pt-1">
                    +{forecasts.length - 6} more
                  </p>
                )}
              </div>
            ) : forecasts ? (
              <p className="text-sm text-muted-foreground">No forecasts available.</p>
            ) : (
              <p className="text-sm text-muted-foreground">Loading...</p>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
