package indicators;
import java.util.List;

public class DBB implements Indicator {
    private double closingPrice;
    private double standardDeviation;
    private final int period;
    private double upperBand;
    private double upperMidBand;
    private double middleBand;
    private double lowerMidBand;
    private double lowerBand;
    private String explanation;
    private SMA sma;

    public DBB(List<Double> closingPrices, int period) {
        this.period = period;
        this.sma = new SMA(closingPrices, period);
        init(closingPrices);
    }

    @Override
    public double get() {
        if ((upperBand - lowerBand) / middleBand < 0.05) //Low volatility case
            return 0;
        if (upperMidBand < closingPrice && closingPrice <= upperBand)
            return 1;
        if (lowerBand < closingPrice && closingPrice <= lowerMidBand)
            return -1;
        else
            return 0;
    }

    @Override
    public double getTemp(double newPrice) {
        double tempMidBand = sma.getTemp(newPrice);
        double tempStdev = sma.tempStandardDeviation(newPrice);
        double tempUpperBand = tempMidBand + tempStdev * 2;
        double tempUpperMidBand = tempMidBand + tempStdev;
        double tempLowerMidBand = tempMidBand - tempStdev;
        double tempLowerBand = tempMidBand - tempStdev * 2;
        if ((tempUpperBand - tempLowerBand) / tempMidBand < 0.05) //Low volatility case
            return 0;
        if (tempUpperMidBand < newPrice && newPrice <= tempUpperBand)
            return 1;
        if (tempLowerBand < newPrice && newPrice <= tempLowerMidBand)
            return -1;
        else
            return 0;
    }

    @Override
    public void init(List<Double> closingPrices) {
        if (period > closingPrices.size()) return;

        closingPrice = closingPrices.size() - 2;
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation*2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation*2;

    }

    @Override
    public void update(double newPrice) {
        closingPrice = newPrice;
        sma.update(newPrice);
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation*2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation*2;
    }

    @Override
    public int check(double newPrice) {
        if (getTemp(newPrice) == 1) {
            explanation = "Price in DBB buy zone";
            return 1;
        }
        if (getTemp(newPrice) == -1) {
            explanation = "Price in DBB sell zone";
            return -1;
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
