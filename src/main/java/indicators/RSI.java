package indicators;

import trading.Formatter;
import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;

public class RSI implements Indicator {
    private double avgUp;
    private double avgDwn;
    private double prevClose;
    private final int period;
    private String explanation;

    public RSI(List<BinanceCandlestick> candles, int period) {
        avgUp = 0;
        avgDwn = 0;
        this.period = period;
        explanation = "";
        init(candles);
    }

    @Override
    public void init(List<BinanceCandlestick> candles) {
        prevClose = candles.get(0).getClose().doubleValue();
        for (int i = 1; i < period + 1; i++) {
            double change = candles.get(i).getClose().doubleValue() - prevClose;
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
        for (int i = period + 1; i < candles.size() - 1; i++) {
            update(candles.get(i).getClose().doubleValue());
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
        if (temp < 30) {
            explanation = "RSI of " + Formatter.formatDecimal(temp);
            return 1;
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
