package indicators;
import java.util.List;

public class BB implements Indicator{
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

    public BB(List<Double> closingPrices, int period) {
        this.period = period;
        this.sma = new SMA(closingPrices, period);
        init(closingPrices);
    }

    @Override
    public double get() {
        if (upperBand < closingPrice)
            return 5;
        if (upperMidBand < closingPrice && closingPrice <= upperBand)
            return 4;
        if (middleBand < closingPrice && closingPrice <= upperMidBand)
            return 3;
        if (lowerMidBand< closingPrice && closingPrice <= middleBand)
            return 2;
        if (lowerBand < closingPrice && closingPrice <= lowerMidBand)
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
        double tempUpperMidBand = tempMidBand + tempStdev;
        double tempLowerMidBand = tempMidBand - tempStdev;
        double tempLowerBand = tempMidBand - tempStdev*2;
        if (tempUpperBand < newPrice)
            return 5;
        if (tempUpperMidBand < newPrice && newPrice <= tempUpperBand)
            return 4;
        if (tempMidBand < newPrice && newPrice <= tempUpperMidBand)
            return 3;
        if (tempLowerMidBand< newPrice && newPrice <= tempMidBand)
            return 2;
        if (tempLowerBand < newPrice && newPrice <= tempLowerMidBand)
            return 1;
        if (newPrice <= tempLowerBand)
            return 0;
        else
            return -1;
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
        /*if (get() == 2 && getTemp(newPrice) == 3) {
            explanation = "Price crossing from below to above upper BB";
            return 1;
        }*/
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
