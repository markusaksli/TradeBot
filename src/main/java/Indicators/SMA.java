package Indicators;

import com.google.common.collect.EvictingQueue;
import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;
import java.util.Queue;

public class SMA implements Indicator {

    private double currentSMA;
    private final int period;
    private Queue<Double> candleValues;

    public SMA(List<BinanceCandlestick> candles, int period) {
        this.period = period;
        candleValues = EvictingQueue.create(period);
        init(candles);
    }

    @Override
    public double get() {
        return currentSMA;
    }

    @Override
    public double getTemp(double newPrice) {
        double oldestPrice = candleValues.element();
        return ((newPrice + currentSMA - oldestPrice) / (double) period);
    }

    @Override
    public void init(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        //Initial SMA
        for (int i = 0; i < period; i++) {
            candleValues.add(candles.get(i).close.doubleValue());
            currentSMA += (candles.get(i).close.doubleValue());
        }

        currentSMA = currentSMA / (double) period;
    }

    @Override
    public void update(double newPrice) {

        currentSMA = 0;
        candleValues.add(newPrice);
        for (Double candleValue : candleValues) {
            currentSMA += candleValue;
        }

        currentSMA = currentSMA / (double) period;

        //Loop could be avoided by using the lines below
        /*
        double oldestPrice = candleValues.element();
        currentSMA = ((newPrice + currentSMA - oldestPrice) / (double) period);
        candleValues.add(newPrice);
         */


    }
}
