package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;

/**
 * EXPONENTIAL MOVING AVERAGE
 */
public class EMA implements Indicator {

    private double currentEMA;
    private final double period;
    private final double multiplier;

    public EMA(List<BinanceCandlestick> candles, double period) {
        this.period = period;
        this.multiplier = 2 / (period + 1);
        setInitial(candles);
    }

    @Override
    public double get() {
        return currentEMA;
    }

    @Override
    public void update(double newPrice) {
        currentEMA = (newPrice - currentEMA) * multiplier + currentEMA;
    }

    @Override
    public void setInitial(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        double initialSMA = 0;

        for (int i = 0; i < period; i++) {
            initialSMA += (candles.get(i).close.doubleValue());
        }
        initialSMA = initialSMA / period;
        double multiplier = 2 / ((double) (period + 1));

        double EMA = initialSMA;
        for (int i = (int) period + 1; i < candles.size(); i++) {
            //EMA = (Close - EMA(previousBar)) * multiplier + EMA(previousBar)
            EMA = (candles.get(i).close.doubleValue() - EMA) * multiplier + EMA;
        }

        currentEMA = EMA;
    }

}
