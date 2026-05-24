package com.financial.tracker.financial_transactions.analytics.prediction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HoltWintersForecasterTest {

    @Test
    void forecast_shortSeries_returnsAverage() {
        double[] series = {100, 110, 105};
        double[] forecast = HoltWintersForecaster.forecast(series, 2, 12);
        assertEquals(2, forecast.length);
        double expectedAvg = (100 + 110 + 105) / 3.0;
        assertEquals(expectedAvg, forecast[0], 0.01);
    }

    @Test
    void forecast_longerSeries_producesValues() {
        double[] series = new double[14];
        for (int i = 0; i < series.length; i++) {
            series[i] = 100 + i * 5;
        }
        double[] forecast = HoltWintersForecaster.forecast(series, 3, 12);
        assertEquals(3, forecast.length);
    }
}
