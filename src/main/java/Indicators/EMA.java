package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;

/**
 * EXPONENTIAL MOVING AVERAGE
 */
public class EMA implements Indicator {

    private double currentEMA;
    private final int period;
    private final double multiplier;

    public EMA(List<BinanceCandlestick> candles, int period) {
        currentEMA = 0;
        this.period = period;
        this.multiplier = 2.0 / (double) (period + 1);
        setInitial(candles);
    }

    @Override
    public double get() {
        return currentEMA;
    }

    @Override
    public double getTemp(double newPrice) {
        return (newPrice - currentEMA) * multiplier + currentEMA;
    }

    @Override
    public void update(double newPrice) {
        // EMA = (Close - EMA(previousBar)) * multiplier + EMA(previousBar)
        currentEMA = (newPrice - currentEMA) * multiplier + currentEMA;
    }

    @Override
    public void setInitial(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        //Initial SMA
        for (int i = 0; i < period; i++) {
            currentEMA += (candles.get(i).close.doubleValue());
        }

        currentEMA = currentEMA / (double) period;

        //Dont use latest unclosed candle;
        for (int i = period + 1; i < candles.size() - 1; i++) {
            update(candles.get(i).getClose().doubleValue());
        }
    }

}
