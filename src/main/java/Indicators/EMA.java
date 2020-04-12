package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;

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

    //Default setting in crypto are period of 9, short 12 and long 26.
    //Three parameters, MACD = 12 EMA - 26 EMA and compare to 9 EMA

    @Override
    public void setInitial(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        //Initial SMA
        for (int i = 0; i < period; i++) {
            currentEMA += (candles.get(i).close.doubleValue());
        }

        currentEMA = currentEMA / (double) period;

        //Dont use latest unclosed candle;
        for (int i = period; i < candles.size() - 1; i++) {
            update(candles.get(i).getClose().doubleValue());
        }
    }

}
