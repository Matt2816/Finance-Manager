package com.financial.tracker.financial_transactions.analytics.prediction;

import java.util.Arrays;

/**
 * Simple additive Holt-Winters triple exponential smoothing for monthly series.
 */
public final class HoltWintersForecaster {

    private HoltWintersForecaster() {
    }

    public static double[] forecast(double[] series, int horizon, int seasonLength) {
        if (series == null || series.length < seasonLength * 2) {
            double avg = series == null || series.length == 0
                    ? 0
                    : Arrays.stream(series).average().orElse(0);
            double[] result = new double[horizon];
            Arrays.fill(result, avg);
            return result;
        }

        double alpha = 0.3;
        double beta = 0.1;
        double gamma = 0.2;

        int n = series.length;
        double[] level = new double[n];
        double[] trend = new double[n];
        double[] seasonal = new double[n];

        level[0] = series[0];
        trend[0] = series.length > 1 ? series[1] - series[0] : 0;
        for (int i = 0; i < seasonLength && i < n; i++) {
            seasonal[i] = series[i] - level[0];
        }

        for (int t = 1; t < n; t++) {
            int seasonIndex = t - seasonLength;
            double prevSeason = seasonIndex >= 0 ? seasonal[seasonIndex] : 0;
            level[t] = alpha * (series[t] - prevSeason) + (1 - alpha) * (level[t - 1] + trend[t - 1]);
            trend[t] = beta * (level[t] - level[t - 1]) + (1 - beta) * trend[t - 1];
            seasonal[t] = gamma * (series[t] - level[t]) + (1 - gamma) * prevSeason;
        }

        double[] forecasts = new double[horizon];
        for (int h = 1; h <= horizon; h++) {
            int seasonIdx = n - seasonLength + ((h - 1) % seasonLength);
            double season = seasonIdx >= 0 && seasonIdx < n ? seasonal[seasonIdx] : 0;
            forecasts[h - 1] = level[n - 1] + h * trend[n - 1] + season;
        }
        return forecasts;
    }
}
