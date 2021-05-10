package indicators;

import java.util.ArrayList;
import java.util.List;

/**
 * EXPONENTIAL MOVING AVERAGE
 */
public class EMA implements Indicator {
    private final int period;
    private final double multiplier;
    private final List<Double> history;
    private final boolean historyNeeded;

    private double currentEMA;

    public EMA(List<Double> warmupData, int period, boolean historyNeeded) {
        currentEMA = 0;
        this.period = period;
        this.historyNeeded = historyNeeded;
        this.multiplier = 2.0 / (double) (period + 1);
        this.history = new ArrayList<>();
        init(warmupData);
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
    public void init(List<Double> warmupData) {
        if (period > warmupData.size()) return;

        //Initial SMA
        for (int i = 0; i < period; i++) {
            currentEMA += warmupData.get(i);
        }

        currentEMA = currentEMA / (double) period;
        if (historyNeeded) history.add(currentEMA);
        //Dont use latest unclosed candle;
        for (int i = period; i < warmupData.size() - 1; i++) {
            update(warmupData.get(i));
        }
    }

    @Override
    public void update(double newPrice) {
        currentEMA = (newPrice - currentEMA) * multiplier + currentEMA;

        if (historyNeeded) history.add(currentEMA);
    }

    @Override
    public int check(double newPrice) {
        return 0;
    }

    @Override
    public String getExplanation() {
        return null;
    }

    public List<Double> getHistory() {
        return history;
    }

    public int getPeriod() {
        return period;
    }
}
