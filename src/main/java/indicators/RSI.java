package indicators;

import data.config.RsiConfig;
import system.Formatter;

import java.util.List;

//Common period for RSI is 14
public class RSI implements Indicator {
    private final RsiConfig config;
    private final int period;

    private double avgUp;
    private double avgDwn;
    private double prevClose;
    private String explanation;

    public RSI(List<Double> warmupData, RsiConfig config) {
        avgUp = 0;
        avgDwn = 0;
        explanation = "";
        this.config = config;
        this.period = config.getPeriod();
        init(warmupData);
    }

    @Override
    public void init(List<Double> warmupData) {
        prevClose = warmupData.get(0);
        for (int i = 1; i < period + 1; i++) {
            double change = warmupData.get(i) - prevClose;
            if (change > 0) {
                avgUp += change;
            } else {
                avgDwn += Math.abs(change);
            }
        }

        //Initial SMA values
        avgUp = avgUp / (double) period;
        avgDwn = avgDwn / (double) period;

        //Dont use latest unclosed value
        for (int i = period + 1; i < warmupData.size() - 1; i++) {
            update(warmupData.get(i));
        }
    }

    @Override
    public double get() {
        return 100 - 100.0 / (1 + avgUp / avgDwn);
    }

    @Override
    public double getTemp(double newPrice) {
        double change = newPrice - prevClose;
        double tempUp;
        double tempDwn;
        if (change > 0) {
            tempUp = (avgUp * (period - 1) + change) / (double) period;
            tempDwn = (avgDwn * (period - 1)) / (double) period;
        } else {
            tempDwn = (avgDwn * (period - 1) + Math.abs(change)) / (double) period;
            tempUp = (avgUp * (period - 1)) / (double) period;
        }
        return 100 - 100.0 / (1 + tempUp / tempDwn);
    }

    @Override
    public void update(double newPrice) {
        double change = newPrice - prevClose;
        if (change > 0) {
            avgUp = (avgUp * (period - 1) + change) / (double) period;
            avgDwn = (avgDwn * (period - 1)) / (double) period;
        } else {
            avgUp = (avgUp * (period - 1)) / (double) period;
            avgDwn = (avgDwn * (period - 1) + Math.abs(change)) / (double) period;
        }
        prevClose = newPrice;
    }

    @Override
    public int check(double newPrice) {
        double temp = getTemp(newPrice);
        if (temp < config.getPositiveMin()) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return 2 * config.getWeight();
        }
        if (temp < config.getPositiveMax()) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return config.getWeight();
        }
        if (temp > config.getNegativeMax()) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return -2 * config.getWeight();
        }
        if (temp > config.getNegativeMin()) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return -config.getWeight();
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
