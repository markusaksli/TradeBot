package indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.LinkedList;
import java.util.List;

public class SMA implements Indicator {

    private double currentSum;
    private final int period;
    private final LinkedList<Double> prices;

    public SMA(List<BinanceCandlestick> candles, int period) {
        this.period = period;
        prices = new LinkedList<>();
        init(candles);
    }

    @Override
    public double get() {
        return currentSum / (double) period;
    }

    @Override
    public double getTemp(double newPrice) {
        return ((currentSum - prices.get(0) + newPrice) / (double) period);
    }

    @Override
    public void init(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        //Initial sum
        for (int i = candles.size() - period - 1; i < candles.size() - 1; i++) {
            prices.add(candles.get(i).close.doubleValue());
            currentSum += (candles.get(i).close.doubleValue());
        }
    }

    @Override
    public void update(double newPrice) {
        currentSum -= prices.get(0);
        prices.removeFirst();
        prices.add(newPrice);
        currentSum += newPrice;
    }

    @Override
    public int check(double newPrice) {
        return 0;
    }

    @Override
    public String getExplanation() {
        return null;
    }
}
