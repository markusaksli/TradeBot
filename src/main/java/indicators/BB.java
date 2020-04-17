package indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;

public class BB implements Indicator{
    private double closingPrice;
    private double standardDeviation;
    private final int period;
    private double upperBand;
    private double middleBand;
    private double lowerBand;
    private SMA sma;

    public BB(List<BinanceCandlestick> candles, int period) {
        this.period = period;
        this.sma = new SMA(candles, period);
        init(candles);
    }

    @Override
    public double get() {
        if (upperBand <= closingPrice)
            return 3;
        if (middleBand < closingPrice && closingPrice < upperBand)
            return 2;
        if (lowerBand < closingPrice && closingPrice <= middleBand)
            return 1;
        if (closingPrice <= lowerBand)
            return 0;
        else
            return -1;
    }

    @Override
    public double getTemp(double newPrice) {
        double tempMidBand = sma.getTemp(newPrice);
        double tempStdev = sma.tempStandardDeviation(newPrice);
        double tempUpperBand = tempMidBand + tempStdev*2;
        double tempLowerBand = tempMidBand - tempStdev*2;
        if (tempUpperBand < newPrice)
            return 3;
        if (tempMidBand < newPrice && newPrice < tempUpperBand)
            return 2;
        if (tempLowerBand < newPrice && newPrice <= tempMidBand)
            return 1;
        if (newPrice < tempLowerBand)
            return 0;
        else
            return -1;
    }

    @Override
    public void init(List<BinanceCandlestick> candles) {
        if (period > candles.size()) return;

        closingPrice = candles.size() - 2;
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation*2;
        lowerBand = middleBand - standardDeviation*2;

    }

    @Override
    public void update(double newPrice) {
        closingPrice = newPrice;
        sma.update(newPrice);
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation*2;
        lowerBand = middleBand - standardDeviation*2;
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
