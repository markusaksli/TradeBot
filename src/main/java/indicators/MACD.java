package indicators;

import data.config.MacdConfig;
import system.Formatter;

import java.util.List;

//Default setting in crypto are period of 9, short 12 and long 26.
//MACD = 12 EMA - 26 EMA and compare to 9 period of MACD value.
public class MACD implements Indicator {
    private final MacdConfig config;
    private final EMA shortEMA; //Will be the EMA object for shortEMA-
    private final EMA longEMA; //Will be the EMA object for longEMA.
    private final int period; //Only value that has to be calculated in setInitial.
    private final double multiplier;
    private final int periodDifference;

    private double currentMACD;
    private double currentSignal;
    private String explanation;

    private double lastTick;

    public MACD(List<Double> warmupData, MacdConfig config) {
        this.config = config;
        this.shortEMA = new EMA(warmupData, config.getShortPeriod(), true); //true, because history is needed in MACD calculations.
        this.longEMA = new EMA(warmupData, config.getLongPeriod(), true); //true for the same reasons.
        this.period = config.getSignalPeriod();
        this.multiplier = 2.0 / (double) (period + 1);
        this.periodDifference = config.getLongPeriod() - config.getShortPeriod();
        explanation = "";
        init(warmupData); //initializing the calculations to get current MACD and signal line.
    }

    @Override
    public double get() {
        return currentMACD - currentSignal; //Difference between the values.
    }

    @Override
    public double getTemp(double newPrice) {
        //temporary values
        double longTemp = longEMA.getTemp(newPrice);
        double shortTemp = shortEMA.getTemp(newPrice);

        double tempMACD = shortTemp - longTemp;
        double tempSignal = tempMACD * multiplier + currentSignal * (1 - multiplier);
        return tempMACD - tempSignal; //Getting the difference between the two signals.
    }

    @Override
    public void init(List<Double> warmupData) {
        //Initial signal line
        //i = longEMA.getPeriod(); because the sizes of shortEMA and longEMA are different.
        for (int i = longEMA.getPeriod(); i < longEMA.getPeriod() + period; i++) {
            //i value with shortEMA gets changed to compensate the list size difference
            currentMACD = shortEMA.getHistory().get(i + periodDifference) - longEMA.getHistory().get(i);
            currentSignal += currentMACD;
        }
        currentSignal = currentSignal / (double) period;

        //Everything after the first calculation of signal line.
        for (int i = longEMA.getPeriod() + period; i < longEMA.getHistory().size(); i++) {
            currentMACD = shortEMA.getHistory().get(i + periodDifference) - longEMA.getHistory().get(i);
            currentSignal = currentMACD * multiplier + currentSignal * (1 - multiplier);
        }

        lastTick = get();
    }

    @Override
    public void update(double newPrice) {
        //Updating the EMA values before updating MACD and Signal line.
        lastTick = get();
        shortEMA.update(newPrice);
        longEMA.update(newPrice);
        currentMACD = shortEMA.get() - longEMA.get();
        currentSignal = currentMACD * multiplier + currentSignal * (1 - multiplier);
    }

    @Override
    public int check(double newPrice) {
        double change = (getTemp(newPrice) - lastTick) / Math.abs(lastTick);
        if (change > config.getRequiredChange() && get() < 0) {
            explanation = "MACD histogram grew by " + Formatter.formatPercent(change);
            return config.getWeight();
        }
        /*if (change < -config.getRequiredChange()) {
            explanation = "MACD histogram fell by " + Formatter.formatPercent(change);
            return -config.GetWeight();
        }*/
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
