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

    public double standardDeviation(){
        double mean = currentSum/ (double) period;
        double stdev = 0.0;
        for (double price : prices) {
            stdev += Math.pow(price-mean, 2);
        }
        return Math.sqrt(stdev/ (double) period);
    }

    public double tempStandardDeviation(double newPrice){

        double tempMean = (currentSum-prices.get(0) + newPrice) / (double) period;
        double tempStdev = 0.0;

        for (int i = 1; i < prices.size(); i++) {
            tempStdev += Math.pow(prices.get(i) - tempMean, 2);
        }

        tempStdev += Math.pow(newPrice - tempMean, 2);
        return Math.sqrt(tempStdev/ (double) period);

    }
}
