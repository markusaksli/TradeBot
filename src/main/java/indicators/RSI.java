package indicators;

import collection.Database;
import trading.Formatter;

import java.util.List;

public class RSI implements Indicator {

    private double avgUp;
    private double avgDwn;
    private double prevClose;
    private final int period;
    private String explanation;
    private static int positiveMin;
    private static int positivseMax;
    private static int negativeMin;
    private static int negativeMax;

    public RSI(List<Double> closingPrice, int period) {
        avgUp = 0;
        avgDwn = 0;
        this.period = period;
        explanation = "";
        init(closingPrice);
    }

    public double getAvgUp() {
        return avgUp;
    }

    public double getAvgDwn() {
        return avgDwn;
    }

    public static void setPositiveMin(int positiveMin) {
        RSI.positiveMin = positiveMin;
    }

    public static void setPositivseMax(int positivseMax) {
        RSI.positivseMax = positivseMax;
    }

    public static void setNegativeMin(int negativeMin) {
        RSI.negativeMin = negativeMin;
    }

    public static void setNegativeMax(int negativeMax) {
        RSI.negativeMax = negativeMax;
    }

    @Override
    public void init(List<Double> closingPrices) {
        prevClose = closingPrices.get(0);
        for (int i = 1; i < period + 1; i++) {
            double change = closingPrices.get(i) - prevClose;
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
        for (int i = period + 1; i < closingPrices.size() - 1; i++) {
            update(closingPrices.get(i));
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
        if (temp < positiveMin) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return 2;
        }
        if (temp < positivseMax) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return 1;
        }
        if (temp > negativeMin) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return -1;
        }
        if (temp > negativeMax) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return -2;
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
